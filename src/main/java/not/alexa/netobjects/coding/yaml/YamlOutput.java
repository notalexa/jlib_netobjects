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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import not.alexa.netobjects.coding.yaml.Yaml.OutputHandler;

/**
 * Handler producing output in YAML style with the following properties:
 * <ul>
 * <li>Encoding is always UTF-8.
 * <li>Indentation is fixed to two spaces.
 * <li>Generic line break is fixed to {@code \n}.
 * <li>Scalars are encoded in plain or single or double quoted single line style.
 * </ul> 
 * Generating an explicit document boundary marker at the beginning of the file can be configured.
 * 
 * @author notalexa
 *
 */
public class YamlOutput implements OutputHandler {
	private static String SPACES="";
	private static String[] PREDEFINED_SPACES=new String[10];
	private static String[] PREDEFINED_ARRAYS=new String[10];
	static {
		for(int i=0;i<10;i++) {
			PREDEFINED_SPACES[i]=spaces(i);
			PREDEFINED_ARRAYS[i]=arrayIndent(i);
		}
	}
	
	private static String spaces(int len) {
		if(len<10&&PREDEFINED_SPACES[len]!=null) {
			return PREDEFINED_SPACES[len];
		}
		while(2*len>SPACES.length()) {
			SPACES+="                                 ";
		}
		return SPACES.substring(0, 2*len);
	}

	private static String arrayIndent(int len) {
		if(len<10&&PREDEFINED_ARRAYS[len]!=null) {
			return PREDEFINED_ARRAYS[len];
		}
		return len==0?"":spaces(len-1)+"- ";
	}

	
	private Writer stream;
	private boolean somethingSeen;
	private Stack<OutputEntry> arrays=new Stack<>();
	private boolean headerWritten;
	private boolean alwaysGenerateStartMarker;
	
	/**
	 * 
	 * General constructor of this output handler
	 * 
	 * @param stream the output stream
	 * @param alwaysGenerateStartMarker if {@code true}, a document boundary marker is generated at the beginning of a file even if not necessary.
	 */
	public YamlOutput(OutputStream stream,boolean alwaysGenerateStartMarker) {
		this.stream=new OutputStreamWriter(stream,Charset.forName("UTF-8"));
		this.alwaysGenerateStartMarker=alwaysGenerateStartMarker;
		arrays.push(new OutputEntry(true,null));
	}
	
	@Override
	public void beginDocument() {
	}

	private void checkHeader(List<Token> modifier) throws IOException {
		if(!headerWritten&&(somethingSeen||modifier.size()>0||alwaysGenerateStartMarker)) {
			stream.append("---");
			for(Token m:modifier) {
				stream.append(' ');
				switch(m.getType()) {
					case Anchor:stream.append('&');
						break;
					case Script:stream.append('@');
						break;
					default:throw new YamlException("Not a modifier: "+m.getType());
				}
				stream.append(m.getValue());
			}
			stream.append("\n");
		}
		headerWritten=somethingSeen=true;
	}

	@Override
	public void endDocument() {
		try {
			headerWritten=false;
			stream.flush();
		} catch(Throwable t) {
		}
	}
	
	private void appendIndent(boolean scalar,boolean key,OutputEntry entry) throws IOException {
		if(entry.scalarKeyWritten) {
			stream.append(": ");
			entry.scalarKeyWritten=false;
		} else {
			if(!entry.empty) {
				stream.append(entry.outputIndent());
			}
			if(!key||!scalar) {
				stream.append(key?"? ":": ");
			}
		}
	}

	@Override
	public void beginArray(boolean key, List<Token> modifier) throws YamlException {
		boolean flag=false;
		boolean empty=true;
		try {
			checkHeader(modifier);
			OutputEntry current=arrays.peek();
			if(!current.array) {
				appendIndent(false,key,current);
				empty=false;
			} else if(arrays.size()>1&&modifier.size()>0) {
				appendIndent(true,true,current);
				empty=false;
				flag=true;
			}
			if(arrays.size()>1&&appendModifier(modifier)) {
				empty=false;
				flag=true;
			}
			if(!empty) {
				stream.write('\n');
			}
			current.empty=false;
			current=arrays.push(new OutputEntry(true,current));
			if(flag) {
				current.outputIndent();
				current.empty=false;
			}
		} catch(IOException e) {
			YamlException.throwException(e);
		}
	}

	@Override
	public void endArray(boolean key) throws YamlException {
		if(arrays.pop().empty&&!arrays.isEmpty()) try {
			stream.append("[]\n");
		} catch(IOException t) {
			YamlException.throwException(t);
		}
	}

	@Override
	public void beginObject(boolean key, List<Token> modifier) throws YamlException {
		boolean empty=true;
		try {
			checkHeader(modifier);
			OutputEntry current=arrays.peek();
			if(current.array&&arrays.size()>1) {
				stream.append(current.indent());
			} else if(current.scalarKeyWritten) {
				stream.append(":");
				empty=false;
				current.scalarKeyWritten=false;
			} else if(arrays.size()>1) {
				appendIndent(false, key,current);
				empty=false;
			}
			if(arrays.size()>1&&appendModifier(modifier)) {
				empty=false;
			}
			if(!empty) {
				stream.append("\n");
			}
			current.empty=false;
			current=arrays.push(new OutputEntry(false,current));
			current.empty=empty;
		} catch(IOException e) {
			YamlException.throwException(e);
		}
	}

	@Override
	public void endObject(boolean key) throws YamlException {
		if(arrays.pop().empty&&!arrays.isEmpty()) try {
			stream.append("{}\n");
		} catch(IOException t) {
			YamlException.throwException(t);
		}
	}
	
	private boolean appendModifier(List<Token> modifier) throws IOException {
		if(modifier!=null&&modifier.size()>0) for(Token m:modifier) {
			switch(m.getType()) {
				case Anchor:stream.append('&');
					break;
				case Script:stream.append('@');
					break;
				default:throw new YamlException("Not a modifier: "+m.getType());
			}
			stream.append(m.getValue()).append(' ');
		}
		return modifier!=null&&!modifier.isEmpty();
	}

	@Override
	public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
		try {
			checkHeader(Collections.emptyList());
			OutputEntry current=arrays.peek();
			if(current.array) {
				if(arrays.size()>1) {
					stream.append(current.outputIndent());
				}
			} else if(!current.empty) {
				appendIndent(true,key,current);
			}
			appendModifier(modifier);
			String v=token.getValue();
			switch(token.getType()) {
				case Scalar: v=Yaml.encode(v);
					if(token.getTag()!=null) {
						switch(v.length()>0?v.charAt(0):' ') {
							case '\'':
							case '"':
								break;
							default:stream.append('!').append(token.getTag()).append(' ');
						}
					}
					stream.append(v);
					break;
				case Alias:stream.append('*').append(v);
					break;
				case Anchor:
				case Script:throw new YamlException("Misplaced modifier "+token.toString()+" in output stream.");
				default:throw new YamlException("Not a scalar: "+token.getType());
			}
			if(!key) {
				stream.append('\n');
			}
			current.scalarKeyWritten=key;
			current.empty=false;
		} catch(IOException e) {
			YamlException.throwException(e);
		}
	}
	
	@Override
	public void flush() throws IOException {
		stream.flush();
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}
	
	private class OutputEntry {
		private boolean scalarKeyWritten;
		private boolean array;
		private boolean empty;
		private String next;
		private String follow;
		private String indent;
		public OutputEntry(boolean array,OutputEntry parent) {
			this.array=array;
			if(parent==null) {
				follow=indent=next="";
			} else {
				int level=1+(parent.next.length()>>1);
				if(array) {
					next=spaces(level);
					follow=arrayIndent(level);
					if(level>1&&parent.array) {
						indent=parent.indent()+"- ";
					} else {
						indent=follow;
					}
				} else {
					if(parent.array) {
						// Same level as the last array
						level--;
					}
					next=spaces(level);
					follow=indent=spaces(level);
				}
			}
			empty=true;
		}
		
		public String indent() {
			return indent;
		}
		public String outputIndent() {
			try {
				return indent();
			} finally {
				indent=follow;
			}
		}
	}
}
