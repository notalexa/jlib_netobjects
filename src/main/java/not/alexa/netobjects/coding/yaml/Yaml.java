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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiConsumer;

import not.alexa.netobjects.coding.text.LineReader;
import not.alexa.netobjects.coding.yaml.Token.SimpleToken;
import not.alexa.netobjects.coding.yaml.Token.Type;

/**
 * Base class for YAML processing. This implementation supports YAML 1.1 with "scripting extension". One of 
 * three {@link Mode}s controls the overall behaviour.
 * <br>The basic output is a {@link Document}, which can be processed using a simple event mechanism provided in
 * {@link Handler}. Nodes represented by a {@link Token} represents documents and this class provides 
 * iterators over documents out of an input stream which can be processed using a handler.
 * <p>Error handling is controlled using the {@link Handler#onError(YamlException)} method. If this method throws an
 * exception, processing is stopped and the exception is globally thrown. If the method doesn't throw an exception,
 * the current document is skipped and the processing continues with the next document (if any).
 * <br>Therefore, the typical usage pattern is
 * <pre>
 * Handler handler=...
 * try(InputStream in=...) {
 *   for(Document doc:yaml.parse(in)) {
 *     doc.process(handler);
 *   }
 * }
 * </pre>
 * and
 * <pre>
 * Handler handler=...
 * Token node=...
 * node.asDocument().process(handler);
 * </pre>
 * if possible scripting modifiers should be handled by the handler or
 * <pre>
 * Handler handler=...
 * Token node=...
 * node.asDocument(yaml).process(handler);
 * </pre>
 * if possible scripting modifiers should be handled by the Yaml instance.
 * <p>Scripts starts with an {@literal @} and can be used at any position where anchors are allowed.
 * Roughly, the script takes a node, transforms it and writes the result to the original handler. Technically,
 * the script should implement the {@link YamlScript} interface and receives the input node
 * as a sequence of events and writes events to the original handler. The packages provides two scripts:
 * <ul>
 * <li>The identity script {@code @identity}.
 * <li>The include script {@code @include} takes scalars (or arrays of (arrays of...)) scalars and interprets
 * them as URLs pointing to a YAML file which is included. The special protocol {@code cp://} denotes the class loaders
 * resource stream mechanism
 * </ul>
 * Anchors and aliases are supported but the restrictions are weakened. First, anchors can be mixed with scripts but note,
 * that anchors after the script are shifted to the script while anchors before the script are attached to the first generated
 * script. Furthermore, we have
 * <ul>
 * <li>no restriction to the number of anchors for a node.
 * <li>no restriction on balancing anchors and aliases. We believe, it't the task of the consumer to handle this properly.
 * <li>no restriction on recursion. Since aliases are handled like other nodes and are not resolved by the processor, there is no
 * danger of infinite recursion.
 * </ul>
 * 
 * The package provides the following implementations of {@link Handler}.
 * <ul>
 * <li>{@link #NOOP} is the no operation handler, ignoring all events
 * <li>{@link DefaultHandler} takes incoming events and constructs a (complex) token
 * which can be consumed using the provided consumer. A typical example
 * would be
 * <pre>
 * List<Token> parse(InputStream stream) throws IOException {
 *   List<Token> result=new ArrayList<>();
 *   Handler handler=new DefaultHandler(true, // fail on error
 *     (t,e)-&gt; {
 *       if(t!=null) {
 *         result.add(t);
 *       }
 *     });
 *   for(Document doc:new Yaml(Mode.Indented).parse(stream)) {
 *     doc.process(handler);
 *   }
 *   return result;
 * }
 * </pre>
 * <li>{@link Delegator} is the typical delegate implementation intended to be overridden.
 * <li>{@link JsonOutput} for outputting documents in JSON format.
 * <li>{@link YamlOutput} for outputting document in YAML (indented) format.
 * </ul>
 * 
 * 
 * 
 * @author notalexa
 *
 */
public class Yaml {
	/**
	 * No operation handler
	 */
	public static Handler NOOP=new Handler() {
		
	};
	
	final Map<String,YamlScript> scripts=new HashMap<>();
	final Mode mode;
	
	/**
	 * 
	 * @param mode the mode of this instance
	 * @param scripts the scripts of this instance. Note that the script are (silently) ignored if the mode is {@link Mode#Json}.
	 */
	public Yaml(Mode mode,YamlScript...scripts) {
		this.mode=mode;
		if(mode!=Mode.Json) for(YamlScript script:scripts) {
			this.scripts.put(script.getName(), script);
		}
	}
	
	/**
	 * Create an output handler depending on the mode. This is one of {@link JsonOutput} or {@link YamlOutput}.
	 * 
	 * @param indent the indentation (currently not used for YAML output)
	 * @param lineFeed the line feed format (currently not used for YAML output)
	 * @param stream the output stream
	 * @return a handler writing documents into the output stream
	 */
	public OutputHandler createOutput(String indent,String lineFeed,OutputStream stream) {
		switch(mode) {
		case Json:
		case ExtendedJson:return new JsonOutput(mode==Mode.ExtendedJson,indent,lineFeed,stream);
		default:return new YamlOutput(stream,false);
		}
	}
	
	/**
	 * 
	 * @param c the character to check
	 * @return {@code true} if the character is printable in the YAML sense
	 */
	public static boolean printable(char c) {
		switch(c) {
		case 0x9:
		case 0xa:
		case 0xd:
		case 0x85: return true;
		default:if(c<0x20) {
				return false;
			} else if(c<0x7f) {
				return true;
			} else if(c<0xa0) {
				return false;
			} else if(c<0xd800) {
				return true;
			} else if(c<0xe000) {
				return false;
			} else if(c<0xffff) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Encode the string for usage as a scalar in mode 0 and non flow mode
	 * 
	 * @param s the string to encode
	 * @return the encoded string
	 * @see #encode(String, boolean, int)
	 */
	public static String encode(String s) {
		return encode(s,0);
	}
	
	/**
	 * Encode the string for usage as a scalar in non flow mode
	 * 
	 * @param s the string to encode
	 * @param mode the mode
	 * @return the encoded string
	 * @see #encode(String, boolean, int)
	 */
	public static String encode(String s,int mode) {
		return encode(s,false,mode);
	}
	

	/**
	 * Encode the string for usage as a scalar. The priority is plain, single quoted (single line) and double quoted (single line).
	 * Plain format can be <b>excluded</b> setting the zero bit of mode, single quoted can be <b>excluded</b> setting the first bit of mode.
	 * In flow mode, some characters are additionally excluded from plain mode (that is must be quoted).
	 *  
	 * @param s the string to encode
	 * @param flowMode if {@code true}, exclude additional characters from plain format
	 * @param mode defines which formats should be considered:
	 * <ul>
	 * <li>0 considers all three formats.
	 * <li>1 excludes plain format.
	 * <li>2 excludes single quoted format.
	 * <li>3 excludes both plain and single quoted format (that is double quoted is used).
	 * </ul>
	 * 
	 * @return the formatted string
	 */
	public static String encode(String s,boolean flowMode,int mode) {
		if(s!=null&&s.length()>0) {
			// Candidate for all encodings
			switch(s.charAt(0)) {
				case '|':
				case '>':
				case ' ':
				case '&':
				case '@':
				case '*':
				case '!':
				case '\'':
				case '\"': mode|=1;
					break;
			}
			boolean spaceEnding=false;
			boolean hasSingleQuotes=false;
			char[] chars=s.toCharArray();
			for(int i=0;i<chars.length;i++) {
				char c=chars[i];
				if(!printable(c)) {
					mode|=3;
				}
				spaceEnding=false;
				switch(c) {
					case ' ':spaceEnding=true;
						break;
					case '\'':hasSingleQuotes=true;
						break;
					case '{':
					case '[':
					case ']':
					case '}':
					case ',':if(!flowMode) {
						break;
					}
					case '\t':
					case '\n':
					case '\r': mode|=3;
						break;
					case '?':
					case '-':
					case ':': mode|=i==chars.length-1||chars[i+1]==' '?1:0;
						break;
					case '#':mode|=i==0||chars[i-1]==' '?1:0;
						break;
				}
				if(mode>=3) {
					break;
				}
			}
			if(spaceEnding) {
				mode|=1;
			}
			if((mode&1)==0) {
				return s;
			} else if((mode&2)==0) {
				if(hasSingleQuotes) {
					return "'"+s.replace("'","''")+"'";
				} else {
					return "'"+s+"'";
				}
			} else {
				String entity=null;
				StringBuilder builder=new StringBuilder().append('"');
				final int n=chars.length;
				for(int i=0;i<n;i++) {
					char c=chars[i];
					switch(c) {
						case 0:entity="\\0"; break;
						case 7:entity="\\a"; break; 
						case 0xb:entity="\\v"; break;
						case 0x1b:entity="\\e"; break;
						case 0x85:entity="\\N"; break;
						case 0xa0:entity="\\_"; break;
						case '"':entity="\\\""; break;
						case '\\':entity="\\\\"; break;
						case '\b':entity="\\b"; break;
						case '\r':entity="\\r"; break;
						case '\f':entity="\\f"; break;
						case '\t':entity="\\t"; break;
						case '\n':entity="\\n"; break;
						case 0x2028:entity="\\L"; break;
						case 0x2029:entity="\\P"; break;
						default:if(!printable(c)) {
								entity=Integer.toHexString(c);
								switch(entity.length()) {
									case 1:builder.append("\\x0");
										break;
									case 2:builder.append("\\x");
										break;
									case 3:builder.append("\\u0");
										break;
									case 4:builder.append("\\u");
										break;
								}
							}
							break;
					}
					if(entity!=null) {
						builder.append(entity);
						entity=null;
					} else {
						builder.append(c);
					}
				}
				return builder.append('"').toString();
			}
		} else {
			return mode>=2?"\"\"":"''";
		}
	}

	/**
	 * Parse and process all documents in the given stream.
	 * 
	 * @param <T> the type of the handler
	 * @param stream the input stream
	 * @param handler the handler to handle documents
	 * @return the handler
	 * @throws IOException if an error occurs
	 */
	public <T extends Handler> T parse(InputStream stream,T handler) throws IOException {
		return parse(new LineReader(stream),handler);
	}
		
	/**
	 * Parse and process all documents in the given reader.
	 * 
	 * @param <T> the type of the handler
	 * @param reader the input stream
	 * @param handler the handler to handle documents
	 * @return the handler
	 * @throws IOException if an error occurs
	 */
	public <T extends Handler> T parse(LineReader reader,T handler) throws IOException {
		for(Document doc:parse(reader)) {
			doc.process(handler);
		}
		return handler;
	}

	/**
	 * Parse the given input stream.
	 * 
	 * @param stream the input stream
	 * @return an iterable over all documents
	 */
	public Iterable<Document> parse(InputStream stream) {
		return parse(new LineReader(stream));
	}

	/**
	 * Parse the input of the given reader
	 * @param reader the input reader
	 * @return an iterable over all documents
	 */
	public Iterable<Document> parse(LineReader reader) {
		return new Iterable<Document>() {
			boolean delivered=false;
			@Override
			public Iterator<Document> iterator() {
				if(delivered) {
					return Collections.emptyIterator();
				} else {
					delivered=true;
					return new YamlProcessor(Yaml.this,reader);
				}
			}
		};
	}
	
	/**
	 * 
	 * @param t the token
	 * @return this token as a document
	 * @throws YamlException if an error occurs (for example because the token is not a node)
	 * @see Token#asDocument(Yaml)
	 */
	public Document asDocument(Token t) throws YamlException {
		return t.asDocument(this);
	}

	/**
	 * 
	 * @return {@code true} if this instance supports scripts.
	 */
	public boolean supportsScripts() {
		return scripts.size()>0;
	}


	/**
	 * Basic interface for handling YAML documents. If not otherwise stated, all default implementations are noops.
	 * 
	 * @author notalexa
	 *
	 */
	public interface Handler {
		
		/**
		 * Begin a document
		 * 
		 * @throws YamlException if the handler reports an error
		 */
		public default void beginDocument() throws YamlException {
		}
		
		/**
		 * End of a document
		 * 
		 * @throws YamlException if the handler reports an error
		 */
		public default void endDocument() throws YamlException {
		}
		
		/**
		 * 
		 * @param key is this array a key?
		 * @param modifier the list of modifiers associated with this array
		 * @throws YamlException if the handler reports an error
		 */
		public default void beginArray(boolean key,List<Token> modifier) throws YamlException {
		}
		
		/**
		 * 
		 * @param key is this array a key? Should match with the last open {@link #beginArray(boolean, List)}
		 * @throws YamlException if the handler reports an error
		 */
		public default void endArray(boolean key) throws YamlException {
		}
		
		/**
		 * 
		 * @param key is this object a key?
		 * @param modifier the list of modifiers associated with this object
		 * @throws YamlException if the handler reports an error
		 */
		public default void beginObject(boolean key,List<Token> modifier) throws YamlException {
		}
		
		/**
		 * 
		 * @param key is this object a key? Should match with the last open {@link #beginObject(boolean, List)
		 * @throws YamlException if the handler reports an error
		 */
		public default void endObject(boolean key) throws YamlException {
		}
		
		/**
		 * 
		 * @param key is this scalar a key?
		 * @param modifier the list of modifiers associated with this scalar
		 * @param token the scalar (or alias)
		 * @throws YamlException if the handler reports an error
		 */
		public default void scalar(boolean key,List<Token> modifier,Token token) throws YamlException {			
		}
		
		/**
		 * Handle the given scalar. The default implementation creates a token and delegates to {@lin Handler#scalar(boolean, List, Token)}.
		 * 
		 * @param key is this scalar a key?
		 * @param scalar the scalar
		 * @throws YamlException if the handler reports an error
		 */
		public default void scalar(boolean key, String scalar) throws YamlException {
			scalar(key, Collections.emptyList(), new SimpleToken(Type.Scalar,null,scalar));
		}
		
		/**
		 * Called whenever an error occurs while processing a document. If thrown, the exception propagates through the {@link Document#process(Handler)}.
		 * 
		 * @param e the exception 
		 * @throws YamlException if the exception should propagate through the document flow
		 */
		public default void onError(YamlException e) throws YamlException {
			throw e;
		}
	}
	
	/**
	 * Basic definition of a (YAML) document.
	 * 
	 * @author notalexa
	 *
	 */
	public interface Document {
		/**
		 * Process this document for the given handler.
		 * 
		 * @param <T> the type of the handler
		 * @param handler the handler used to process the document
		 * @return the handler itself
		 * @throws YamlException if an error occurs which is either fatal (e.g. io errors) or thrown by the {@link Handler#onError(YamlException)} method.
		 */
		public <T extends Handler> T process(T handler) throws YamlException;
	}

	/**
	 * Extension of {@link Handler} to support output (streams)
	 * @author notalexa
	 *
	 */
	public interface OutputHandler extends Handler, Closeable {
		/**
		 * Flush the output stream
		 * @throws IOException if an error occurs
		 */
		public default void flush() throws IOException {
		}
		
		/**
		 * Convenience method for writing a token to the output stream. This is equivalent to
		 * <pre>
		 * t.asDocument().process(this);
		 * </pre>
		 * 
		 * @param t the token to write to the output
		 * @throws YamlException if an error occurs
		 */
		public default void write(Token t) throws YamlException {
			t.asDocument().process(this);
		}
	}
	
	/**
	 * The supported modes of this packages.
	 * 
	 * @author notalexa
	 *
	 */
	public enum Mode {
		/**
		 * Strict Json. No modifiers, no aliases, no object and arrays as keys.
		 */
		Json,
		/**
		 * Extended Json. The documents content should be entirely expressed in "flow mode".
		 */
		ExtendedJson,
		/**
		 *  Normal YAML mode. Both flow and indentation mode are accepted. Output is created as indented output where possible.
		 */
		Indented;
	}



	/**
	 * Internal use only. Generate events for the given token.
	 * 
	 * @param yaml the yaml instance (can be {@¢ode null})
	 * @param token the token
	 * @param handler the handler processing the generated events
	 * @throws YamlException if an error occurs which is not handled by the {@link Handler#onError(YamlException)} method
	 */
	static void process0(Yaml yaml,Token token,Handler handler) throws YamlException {
		process0(yaml,handler,new ArrayList<>(1),false,token);
	}
	
	private static void process0(Yaml yaml,Handler handler,List<YamlException> e,boolean key,Token token) throws YamlException {
		token.undecorate((modifier,tok)->{
			try {
				switch(tok.getType()) {
					case Sequence:
						handler.beginArray(key, modifier);
						for(Token t:tok.getArray()) {
							process0(yaml,handler,e,false,t);
						}
						handler.endArray(key);
						break;
					case Map:
						handler.beginObject(key, modifier);
						for(Map.Entry<Token,Token> entry:tok.getMapArray()) {
							process0(yaml,handler,e,true,entry.getKey());
							process0(yaml,handler,e,false,entry.getValue());
						}
						handler.endObject(key);
						break;
					default: handler.scalar(key,modifier,tok);
				}
			} catch(YamlException ex) {
				e.add(ex);
			}
		});
		if(e.size()>0) {
			throw e.get(0);
		}
	}	

	/**
	 * Produce a complex token out of the event stream. This handler creates {@link MapToken}s and {@link SequenceToken}s and decorates the scalars as
	 * appropriate.
	 * <br>The final token and errors can be consumed using the provided consumer.
	 * 
	 * @author notalexa
	 *
	 */
	public static class DefaultHandler implements Handler {
		private Stack<Entry> stack=new Stack<>();
		private BiConsumer<Token,Throwable> callback;
		private boolean failOnError;
		private Token result;

		/**
		 * Construct this handler
		 * @param failOnError if{@code true}, the handler fails on any error. Otherwise, the document under construction is skipped.
		 * @param callback the callback for result or errors
		 */
		public DefaultHandler(boolean failOnError,BiConsumer<Token,Throwable> callback) {
			this.failOnError=failOnError;
			this.callback=callback;
		}

		@Override
		public void beginDocument() throws YamlException {
			if(!stack.isEmpty()) {
				throw new YamlException("Incomplete predecessor");
			}
			result=null;
		}

		@Override
		public void endDocument() throws YamlException {
			try {
				if(!stack.isEmpty()) {
					throw new YamlException("Incomplete document");
				}
				callback.accept(result, null);
				result=null;
			} finally {
				result=null;
				stack.clear();
			}
		}
		
		private void setResult(Token result) throws YamlException {
			if(this.result!=null) {
				throw new YamlException("Misplaced token. Result already defined");
			}
			this.result=result;
		}

		@Override
		public void beginArray(boolean key, List<Token> modifier) throws YamlException {
			Entry entry=new Entry(key,modifier,true);
			if(stack.isEmpty()) {
				setResult(entry.getToken());
			} else {
				stack.peek().setToken(key, entry.getToken());
			}
			stack.push(entry);
		}
		
		@Override
		public void endArray(boolean key) throws YamlException {
			Entry entry=stack.pop();
			if(key!=entry.key) {
				throw new YamlException("Key value mismatch");
			}
		}
		
		@Override
		public void beginObject(boolean key, List<Token> modifier) throws YamlException {
			Entry entry=new Entry(key,modifier,false);
			if(stack.isEmpty()) {
				setResult(entry.getToken());
			} else {
				stack.peek().setToken(key, entry.getToken());
			}
			stack.push(entry);
		}

		@Override
		public void endObject(boolean key) throws YamlException {
			Entry entry=stack.pop();
			if(key!=entry.key) {
				throw new YamlException("Key value mismatch");
			}
		}

		@Override
		public void scalar(boolean key,List<Token> modifier,Token token) throws YamlException {
			if(stack.isEmpty()) {
				setResult(token.decorate(modifier));
			} else {
				stack.peek().setToken(key, token.decorate(modifier));
			}
		}
		
		@Override
		public void onError(YamlException e) throws YamlException {
			callback.accept(null, e);
			stack.clear();
			result=null;
			if(failOnError) {
				Handler.super.onError(e);
			}
		}

		private class Entry {
			boolean key;
			Token keyToken;
			SequenceToken array;
			MapToken map;
			Token result;
			
			public Entry(boolean key,List<Token> modifier,boolean array) throws YamlException {
				this.key=key;
				if(array) {
					this.array=new SequenceToken();
					result=this.array.decorate(modifier);
				} else {
					map=new MapToken();
					result=map.decorate(modifier);
				}
			}
			
			void setToken(boolean key,Token t) throws YamlException {
				if(array!=null) {
					array.add(t);
				} else if(key) {
					if(keyToken!=null) {
						throw new YamlException("Misplaced key");
					}
					keyToken=t;
				} else {
					if(keyToken==null) {
						throw new YamlException("Misplaced value");
					}
					map.add(keyToken, t);
					keyToken=null;
				}
			}
			
			Token getToken() {
				return result;
			}
		}
	}
	
	/**
	 * Delegator class für {@link Handler}.
	 * 
	 * @author notalexa
	 *
	 */
	public static class Delegator implements Handler {
		protected Handler delegate;
		
		protected Delegator(Handler delegate) {
			this.delegate=delegate;
		}

		@Override
		public void beginDocument() throws YamlException {
			delegate.beginDocument();
		}
		
		@Override
		public void endDocument() throws YamlException {
			delegate.endDocument();
		}

		@Override
		public void beginArray(boolean key, List<Token> modifier) throws YamlException {
			delegate.beginArray(key, modifier);
		}

		@Override
		public void endArray(boolean key) throws YamlException {
			delegate.endArray(key);
		}

		@Override
		public void beginObject(boolean key, List<Token> modifier) throws YamlException {
			delegate.beginObject(key, modifier);
		}

		@Override
		public void endObject(boolean key) throws YamlException {
			delegate.endObject(key);
		}

		@Override
		public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
			delegate.scalar(key, modifier, token);
		}
		
		@Override
		public void onError(YamlException e) throws YamlException {
			delegate.onError(e);
		}
	}
}
