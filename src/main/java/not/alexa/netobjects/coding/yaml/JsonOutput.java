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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import not.alexa.netobjects.coding.yaml.Yaml.OutputHandler;

/**
 * Handler for generating YAML in flow mode (JSON). Two modes are accepted:
 * <ul>
 * <li>In not extended mode, modifiers like anchors, alias are not accepted. Furthermore, maps and sequences are rejected as keys.
 * <li>In extended mode, all YAML features are accepted.
 * </ul>
 * Indentation and (generic) line feed style can be specified in the constructor.
 * <br>All scalar tokens are represented as single line double quoted strings accept for the following tags: {@code !int}, {@code !float}, {@code !bool} and {@code !null}.
 * For these types, the default encoding is plain but no further checks are made. Therefore a token with tag {@code !int} and value {@code x} will not
 * be rejected. Consequently, if the string cannot be represented as plain, it is double quoted independently of the tag.
 *    
 * @author notalexa
 *
 */
public class JsonOutput implements OutputHandler {
	private static final Set<String> UNQUOTED_TYPES=new HashSet<>();
	static {
		UNQUOTED_TYPES.add("!bool");
		UNQUOTED_TYPES.add("!float");
		UNQUOTED_TYPES.add("!int");
		UNQUOTED_TYPES.add("!null");		
	}
	private boolean extended;
	private String indent;
	private String lineFeed;
	private Writer stream;
	private boolean somethingSeen=false;
	private boolean empty;
	private Stack<OutputEntry> array=new Stack<>();
	
	/**
	 * Create a flow mode output handler.
	 * 
	 * @param extended should extended output be generated
	 * @param indent the indentation to use
	 * @param lineFeed the line feed to use
	 * @param stream the output stream
	 */
	public JsonOutput(boolean extended,String indent,String lineFeed,OutputStream stream) {
		this.extended=extended;
		this.indent=indent;
		this.lineFeed=lineFeed;
		empty=true;
		this.stream=new OutputStreamWriter(stream,Charset.forName("UTF-8"));
		array.push(new OutputEntry(true,lineFeed));
	}
	
	@Override
	public void beginDocument() throws YamlException {
		if(somethingSeen) {
			if(!extended) {
				throw new YamlException("Not supported in JSON: Multiple Documents");
			} else try {
				stream.append(lineFeed).append("---").append(lineFeed);
			} catch(IOException t) {
				YamlException.throwException(t);
			}
		}
		somethingSeen=true;
		empty=true;
	}

	@Override
	public void endDocument() {
		try {
			stream.flush();
		} catch(IOException e) {
		}
	}

	@Override
	public void beginArray(boolean key, List<Token> modifier) throws YamlException {
		try {
			OutputEntry current=array.peek();
			if(!empty) {
				stream.append(',');
			}
			if(key||(current.array&&array.size()>1)) {
				stream.append(current.indent);
			} else if(!current.array) {
				stream.append(": ");
			}
			if(extended) {
				appendModifier(modifier);
			}
			stream.append('[');
			empty=true;
			array.push(new OutputEntry(true,current.indent+indent));
		} catch(IOException e) {
			YamlException.throwException(e);
		}
	}

	@Override
	public void endArray(boolean key) throws YamlException {
		try {
			array.pop();
			stream.append(array.peek().indent).append(']');
			empty=key;
		} catch(IOException e) {
			YamlException.throwException(e);
		}
	}

	@Override
	public void beginObject(boolean key, List<Token> modifier) throws YamlException {
		try {
			if(!empty) {
				stream.append(',');
			}
			OutputEntry current=array.peek();
			if(key||(current.array&&array.size()>1)) {
				stream.append(current.indent);
			} else if(!current.array) {
				stream.append(": ");
			}
			if(extended) {
				appendModifier(modifier);
			}
			stream.append('{');
			empty=true;
			array.push(new OutputEntry(false,current.indent+indent));
		} catch(IOException e) {
			YamlException.throwException(e);
		}
	}

	@Override
	public void endObject(boolean key) throws YamlException {
		try {
			array.pop();
			stream.append(array.peek().indent).append('}');
			empty=key;
		} catch(IOException e) {
			YamlException.throwException(e);
		}
	}
	
	private void appendModifier(List<Token> modifier) throws IOException {
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
	}

	@Override
	public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
		try {
			if(!empty) {
				stream.write(',');
			}
			OutputEntry current=array.peek();
			if(key||current.array) {
				if(array.size()>1) {
					stream.append(current.indent);
				}
				empty=key;
			} else {
				stream.append(": ");
				empty=false;
			}
			if(extended) {
				appendModifier(modifier);
			}
			switch(token.getType()) {
				case Scalar:stream.append(Yaml.encode(token.getValue(),true,!key&&token.getTag()!=null&&UNQUOTED_TYPES.contains(token.getTag())?2:3));
					break;
				case Alias:if(!extended) {
						throw new IOException("Anchors are not allowed in standard JSON");
					} else {
						stream.append('*').append(token.getValue());
					}
					break;
				case Anchor:
				case Script:throw new YamlException("Misplaced modifier "+token.toString()+" in output stream.");
				default:throw new YamlException("Not a scalar: "+token.getType());
			}
		} catch(IOException e) {
			YamlException.throwException(e);
		}
	}

	@Override
	public void scalar(boolean key, String token) throws YamlException {
		try {
			if(!empty) {
				stream.write(',');
			}
			OutputEntry current=array.peek();
			if(key||current.array) {
				stream.append(current.indent).append(Yaml.encode(token, 3));
				empty=true;
			} else {
				stream.append(": ").append(Yaml.encode(token, 3));
				empty=false;
			}
		} catch(IOException e) {
			YamlException.throwException(e);
		}
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}
	@Override
	public void flush() throws IOException {
		stream.flush();
	}
	
	private static class OutputEntry {
		private boolean array;
		private String indent;
		private OutputEntry(boolean array,String indent) {
			this.array=array;
			this.indent=indent;
		}
	}
}
