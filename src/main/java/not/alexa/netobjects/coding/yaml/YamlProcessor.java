/*
 * Copyright (C) 2023 Not Alexa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package not.alexa.netobjects.coding.yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;

import not.alexa.netobjects.coding.text.LineReader;
import not.alexa.netobjects.coding.text.LineReader.Line;
import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Delegator;
import not.alexa.netobjects.coding.yaml.Yaml.Document;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.coding.yaml.YamlLine.FlowEntry;
import not.alexa.netobjects.coding.yaml.YamlLine.Tokenizer;

/**
 * Package private class for processing YAML documents. The processor is an iterator over documents which can
 * be processed using the {@link Document#process(Handler)} method.
 * 
 * @author notalexa
 *
 */
class YamlProcessor implements Iterator<Document> {
	private Yaml yaml;
	private LineReader reader;
	private Line line=new Line();
	private YamlLine structureIndentation=new YamlLine(YamlProcessor.this) {
		@Override
		public boolean isDocumentStart() {
			return true;
		}
	};
	private YamlLine savedIndentation;
	private Handler handler=Yaml.NOOP;
	private boolean next=false;
	private boolean active;
	private List<Token> modifier=new ArrayList<>();
	private YamlException initException;

	YamlProcessor(Yaml yaml,LineReader reader) {
		this.yaml=yaml;
		this.reader=reader;
		try {
			while(reader.readLine(line)) {
				YamlLine nextIndentation=parse(line,true);
				if(nextIndentation!=null&&!nextIndentation.isDocumentEnd()) {
					if(nextIndentation.isDocumentStart()) {
						structureIndentation=nextIndentation;
					} else {
						savedIndentation=nextIndentation;
					}
					next=true;
					break;
				}
			}
		} catch(Throwable t) {
			// Throw this exception while processing
			next=true;
			initException=normalize(t);
		}
	}
	
	private YamlException normalize(Throwable t) {
		if(t instanceof YamlException) {
			return (YamlException)t;
		} else {
			return new YamlException(t);
		}
	}
	
	public boolean supportsScripts() {
		return yaml.supportsScripts();
	}
			
	public YamlScript getScript(String name) {
		return yaml.scripts.get(name);
	}
	
	@Override
	public boolean hasNext() {
		if(next&&handler==null) {
			// Process the unprocessed last document internally 
			// and move to the next document
			next=cleanup(null);
		}
		return next;
	}
	
	private boolean cleanup(YamlLine l0) {
		if(handler==null||l0!=null) try {
			handler=Yaml.NOOP;
			while(l0!=null||reader.readLine(line)) {
				YamlLine l=l0!=null?l0:parse(line,false);
				l0=null;
				if(l!=null) {
					if(l.isDocumentStart()) {
						structureIndentation=l;
						return true;
					} else if(l.isDocumentEnd()) {
						structureIndentation=l;
						return false;
					}
				}
			}
		} catch(Throwable t) {
		}
		return false;
	}
		
	@Override
	public Document next() {
		handler=null;
		return new Document() {
			boolean processed;
			@Override
			public <T extends Handler> T process(T handler) throws YamlException {
				class MarkerException extends RuntimeException {
					private static final long serialVersionUID = 1L;
				}
				if(initException!=null) {
					processed=true;
					next=false;
					handler.onError(line.fill(initException));
				} else if(processed) {
					handler.onError(new YamlException("Document already processed"));
				} else try {
					processed=true;
					YamlProcessor.this.handler=new ExceptionDecorator(yaml.mode,line,yaml.supportsScripts()?new ScriptChecker(handler):handler) {

						@Override
						public void onError(YamlException e) throws YamlException {
							super.onError(e);
							// If not thrown, we throw a marker exception to clean
							throw new MarkerException();
						}
						
					};
					parse();
				} catch(IOException e) {
					YamlException yamlEx=line.fill(normalize(e));
					next=cleanup(structureIndentation);
					handler.onError(yamlEx);
					//return YamlException.throwException(e);
				} catch(RuntimeException e) {
					next=cleanup(structureIndentation);
					if(!(e instanceof MarkerException)) {
						throw e;
					}
				}
				return handler;
			}
		};
	}

	private void parse() throws IOException {
		try {
			Tokenizer tokenizer=structureIndentation.getTokenizer();
			while(tokenizer.hasNext()) {
				Token t=tokenizer.next();
				if(t.getType()==Type.Anchor) {
					modifier.add(t);
				}
			}
			tokenizer=null;
			outerloop: while(savedIndentation!=null||reader.readLine(line)) {
				if(tokenizer!=null&&tokenizer.isContinued()) {
					tokenizer.update(line);
				} else {
					YamlLine nextIndentation=savedIndentation!=null?savedIndentation:parse(line,true);
					savedIndentation=null;
					if(nextIndentation!=null) {
						tokenizer=update(nextIndentation);
						if(tokenizer==null) {
							return;
						}
					} else {
						continue outerloop;
					}
				}
				while(true) {
					for(;tokenizer.hasNext();) {
						Token token=tokenizer.next();
						consume(tokenizer,token);
					}
					YamlLine nextIndentation=tokenizer.nextLine();
					if(nextIndentation!=null) {
						tokenizer=update(nextIndentation);
						if(tokenizer==null) {
							return;
						}
					} else {
						continue outerloop;
					}
				} 
			}
			if(tokenizer!=null&&tokenizer.isContinued()) {
				tokenizer.eof();
				for(;tokenizer.hasNext();) {
					Token token=tokenizer.next();
					consume(tokenizer,token);
				}
			}
			finish();
			next=false;
		} catch(InternalException e) {
			e.throwYaml();
		}
	}
	
	YamlLine parse(Line input,boolean fully) throws IOException {
		if(input.length>0) {
			if(input.content[0]=='%') {
				String line=input.getLine();
				if(line.startsWith("%YAML ")) {
					String version=line.substring("%YAML ".length()).trim();
					if(!version.startsWith("1.")) {
						return input.fillAndThrow(new YamlException("Unsupported YAML version: "+version));
					}
				} else if(line.startsWith("%TAG")) {
				}
				return null;
			}
		}
		if(input.length>=3) {
			if(input.content[0]=='-'&&input.content[1]=='-'&&input.content[2]=='-') {
				return new YamlLine(YamlProcessor.this) {
					{
						current=input;
						delimiter="---";
						size=3;
					}
					
					@Override
					public boolean isDocumentStart() {
						return true;
					}
				};
			} else if(input.content[0]=='.'&&input.content[1]=='.'&&input.content[2]=='.') {
				return new YamlLine(YamlProcessor.this) {
					{
						current=input;
						delimiter=null;
					}
					
					@Override
					public boolean isDocumentEnd() {
						return true;
					}
				};
			}
		}
		return fully?structureIndentation.parse(input):null;
	}

	private Tokenizer update(YamlLine nextIndentation) throws IOException {
		if(structureIndentation.isDocumentEnd()) {
			handler.onError(new YamlException("Document finished"));
			return null;
		}
		if(nextIndentation.isDocumentStart()||nextIndentation.isDocumentEnd()) try {
			finish();
			return null;
		} finally {
			next&=nextIndentation.isDocumentStart();
			structureIndentation=nextIndentation;
		} else {
			if(!active) {
				handler.beginDocument();
				active=true;
			}
			structureIndentation.generateObjectEvents(nextIndentation,modifier,handler);
		}
		structureIndentation=nextIndentation;
		return structureIndentation.getTokenizer();
	}
	
	private void finish() throws YamlException {
		if(active) {
			structureIndentation.finishObjectEvents(handler);
			handler.endDocument();
			active=false;
		}
	}
	
	private void consume(Tokenizer tokenizer,Token token) throws YamlException {
		if(tokenizer.isFlowMode()) {
			switch(token.getType()) {
				case Script:
				case Anchor:
					modifier.add(token);
					break;
				case Alias:
				case Scalar:
					FlowEntry flowEntry=tokenizer.peekFlowEntry();
					flowEntry.incr();
					handler.scalar(flowEntry.isKey(),modifier, token);
					modifier.clear();
					break;
				case Separator:tokenizer.peekFlowEntry().handleSeparator();
					break;
				case ValueIndicator:tokenizer.peekFlowEntry().handleValueIndicator();
					break;
				case SquareOpen:
				case CurlyOpen:tokenizer.peekFlowEntry().startObject(modifier,handler);
					modifier.clear();
					break;
				case SquareClose:
				case CurlyClose:tokenizer.peekFlowEntry().endObject(token.getType()==Type.SquareClose,handler);
					break;			
			}
		} else {
			switch(token.getType()) {
			case Scalar:
			case Alias:
				if(active) {
					tokenizer.peekIndentationEntry().scalar(modifier,token,handler);
				}
				break;
			case Script:
			case Anchor:
				modifier.add(token);
				break;
			case KeyIndicator:
				tokenizer.peekIndentationEntry().key(modifier,handler);
				break;
			case ValueIndicator:
				tokenizer.peekIndentationEntry().value(handler);
				break;		
			}
		}
	}
	
	private class ScriptHandler extends Delegator implements Handler {
		private List<Token> modifier;		
		private ScriptHandler(List<Token> modifier,Handler delegate) {
			super(delegate);
			this.modifier=new ArrayList<>(modifier);
		}

		private List<Token> modifier(List<Token> modifier) {
			if(this.modifier!=null) try {
				if(modifier.size()>=0) {
					this.modifier.addAll(modifier);
				}
				return this.modifier;
			} finally {
				this.modifier=null;
			}
			return modifier;
		}

		@Override
		public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
			super.scalar(key, modifier(modifier), token);
		}

		@Override
		public void beginArray(boolean key, List<Token> modifier) throws YamlException {
			super.beginArray(key, modifier(modifier));
		}

		@Override
		public void beginObject(boolean key, List<Token> modifier) throws YamlException {
			super.beginObject(key, modifier(modifier));
		}
	}
	
	private class ScriptChecker implements Handler {
		Stack<Handler> delegate=new Stack<Handler>();
		private ScriptChecker(Handler delegate) {
			this.delegate.push(delegate);
		}

		@Override
		public void beginDocument() throws YamlException {
			delegate.peek().beginDocument();
		}

		@Override
		public void endDocument() throws YamlException {
			delegate.peek().endDocument();
		}
		
		private void scripted(List<Token> modifier,BiFunction<Handler, List<Token>,YamlException> callback) throws YamlException {
			List<Token> resolved=new ArrayList<>();
			Handler handler=delegate.peek();
			for(Token mod:modifier) {
				if(mod.getType()==Type.Anchor) {
					resolved.add(mod);
				} else {
					YamlScript script=getScript(mod.getValue());
					if(script!=null) {
						if(resolved.size()>0) {
							handler=new ScriptHandler(resolved,handler);
							resolved.clear();
						}
						handler=script.create(yaml, handler);
					} else {
						line.fillAndThrow(new YamlException("Unsupported script: "+mod.getValue()));
					}
				}
			}
			YamlException e=callback.apply(handler, resolved);
			if(e!=null) {
				line.fillAndThrow(e);
			}
		}

		@Override
		public void beginArray(boolean key, List<Token> modifier) throws YamlException {
			if(modifier.size()>0) {
				scripted(modifier,(handler,m)-> {
					try {
						delegate.push(handler);
						handler.beginArray(key, m);
						return null;
					} catch(YamlException e) {
						return e;
					}
				});
			} else {
				delegate.push(delegate.peek());
				delegate.peek().beginArray(key, modifier);
			}
		}

		@Override
		public void endArray(boolean key) throws YamlException {
			delegate.pop().endArray(key);
		}

		@Override
		public void beginObject(boolean key, List<Token> modifier) throws YamlException {
			if(modifier.size()>0) {
				scripted(modifier,(handler,m)-> {
					try {
						delegate.push(handler);
						handler.beginObject(key, m);
						return null;
					} catch(YamlException e) {
						return e;
					}
				});
			} else {
				delegate.push(delegate.peek());
				delegate.peek().beginObject(key, modifier);
			}
		}

		@Override
		public void endObject(boolean key) throws YamlException {
			delegate.pop().endObject(key);
		}

		@Override
		public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
			if(modifier.size()>0) {
				scripted(modifier,(handler,m)-> {
				try {
						handler.scalar(key, m, token);
						return null;
					} catch(YamlException e) {
						return e;
					}
				});
			} else {
				delegate.peek().scalar(key, modifier, token);
			}
		}

		@Override
		public void onError(YamlException e) throws YamlException {
			while(!delegate.isEmpty()) try {
				delegate.pop().onError(e);
				// Consumed
				return;
			} catch(YamlException e1) {
				e=e1;
			}
			throw e;
		}
	}
	
	static class ExceptionDecorator implements Handler {
		private Mode mode;
		private Handler delegate;
		private Line line;
		ExceptionDecorator(Mode mode,Line line,Handler delegate) {
			this.mode=mode;
			this.line=line;
			this.delegate=delegate;
		}

		@Override
		public void beginDocument() throws YamlException {
			try {
				delegate.beginDocument();
			} catch(YamlException e) {
				onError(e);
			}
		}

		@Override
		public void endDocument() throws YamlException {
			try {
				delegate.endDocument();
			} catch(YamlException e) {
				onError(e);
			}
		}

		@Override
		public void beginArray(boolean key, List<Token> modifier) throws YamlException {
			try {
				if(mode==Mode.Json) {
					if(modifier.size()>0) {
						throw new YamlException("Forbidden in Json: Modifier");
					}
					if(key) {
						throw new YamlException("Forbidden in Json: Arrays as keys");
					}
				}
				delegate.beginArray(key, modifier);
			} catch(YamlException e) {
				onError(e);
			}
		}

		@Override
		public void endArray(boolean key) throws YamlException {
			try {
				delegate.endArray(key);
			} catch(YamlException e) {
				onError(e);
			}
		}

		@Override
		public void beginObject(boolean key, List<Token> modifier) throws YamlException {
			try {
				if(mode==Mode.Json) {
					if(modifier.size()>0) {
						throw new YamlException("Forbidden in Json: Modifier");
					}
					if(key) {
						throw new YamlException("Forbidden in Json: Object as keys");
					}
				}
				delegate.beginObject(key, modifier);
			} catch(YamlException e) {
				onError(e);
			}
		}

		@Override
		public void endObject(boolean key) throws YamlException {
			try {
				delegate.endObject(key);
			} catch(YamlException e) {
				onError(e);
			}
		}

		@Override
		public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
			try {
				if(mode==Mode.Json) {
					if(modifier.size()>0) {
						throw new YamlException("Forbidden in Json: Modifier");
					}
					if(token.getType()!=Type.Scalar) {
						throw new YamlException("Forbidden in Json: Aliases");
					}
				}
				delegate.scalar(key, modifier, token);
			} catch(YamlException e) {
				onError(e);
			}
		}

		@Override
		public void onError(YamlException e) throws YamlException {
			delegate.onError(line.fill(e));
		}
	}
	
	static class InternalException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		InternalException(String msg) {
			super(msg);
		}
		
		private void throwYaml() throws YamlException {
			YamlException e=new YamlException(getMessage());
			e.setStackTrace(getStackTrace());
			throw e;
		}
	}
}
