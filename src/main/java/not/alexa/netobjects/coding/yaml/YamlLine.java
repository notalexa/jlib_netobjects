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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import not.alexa.netobjects.coding.text.LineReader.Line;
import not.alexa.netobjects.coding.yaml.Token.SimpleToken;
import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.coding.yaml.YamlProcessor.InternalException;

/**
 * Internal class handling one line of input.
 * 
 * @author notalexa
 *
 */
class YamlLine {
	private static final Tokenizer EMPTY_TOKENIZER=new Tokenizer() {
		
		@Override
		public Token next() {
			return null;
		}
		
		@Override
		public boolean hasNext() {
			return false;
		}
		
		@Override
		public void update(Line line) {
		}
		
		@Override
		public boolean isContinued() {
			return false;
		}

		@Override
		public YamlLine nextLine() throws IOException {
			return null;
		}

		@Override
		public void eof() throws IOException {
		}
	};
	private static final Token OBJECT_INDICATOR=new Token.SimpleToken(Type.Scalar, null, "");
	private static final Token CURLY_OPEN=new InternalToken(Type.CurlyOpen,"{");
	private static final Token CURLY_CLOSE=new InternalToken(Type.CurlyClose,"}");
	private static final Token SQUARE_OPEN=new InternalToken(Type.SquareOpen,"[");
	private static final Token SQUARE_CLOSE=new InternalToken(Type.SquareClose,"]");
	private static final Token KEY_INDICATOR=new InternalToken(Type.KeyIndicator,"?");
	private static final Token VALUE_INDICATOR=new InternalToken(Type.ValueIndicator,":");
	private static final Token SEPARATOR=new InternalToken(Type.Separator,",");
	protected int size;
	int length;
	private int firstArray=-1;
	protected String delimiter;
	private YamlProcessor structure;
	
	protected Line current;
	private IndentationEntry[] entries;
	private Stack<FlowEntry> level=new Stack<>();
	
	public YamlLine(YamlProcessor structure) {
		this(structure,1);
		entries[0]=new IndentationEntry(null,0,false,false);
		length=1;
	}

	private YamlLine(YamlProcessor structure,int defaultSize) {
		this.structure=structure;
		entries=new IndentationEntry[defaultSize];
	}
	
	boolean isFlowMode() {
		return level.size()>0;
	}
	
	void addLevel(FlowEntry level) {
		this.level.push(level);
	}
	
	FlowEntry currentLevel() {
		return level.isEmpty()?null:level.peek();
	}

	private void set(int index,IndentationEntry entry) {
		if(index>=entries.length) {
			entries=Arrays.copyOf(entries, index+10);
		}
		entries[index]=entry;
	}
	
	boolean isDocumentStart() {
		return false;
	}

	boolean isDocumentEnd() {
		return false;
	}

	Tokenizer getTokenizer() {
		if(delimiter==null||delimiter.length()==0) {
			return EMPTY_TOKENIZER;
		} else {
			return new DefaultTokenizer();
		}
	}
	
	<T extends Handler> void generateObjectEvents(YamlLine next,List<Token> modifier,T handler) throws YamlException {
		int update=next.firstArray<0?length-next.length:length-next.firstArray;
		if(update>0) {
			int offset=Math.min(next.length, next.firstArray);
			for(int i=0;i<update;i++) {
				entries[length-i-1].finish(length-i-1>offset, handler);
			}
		}
		update=next.firstArray<0?next.length-length:next.length-next.firstArray;
		if(update>0) {
			for(int i=0;i<update;i++) {
				next.entries[next.length-update+i].start(modifier, handler);
				modifier.clear();
			}
			modifier.clear();
		} else {
			if(modifier.size()>0) {
				next.entries[next.length-1].updateModifiers(modifier);//System.out.println("Generate: modifier are "+modifier);
			}
			modifier.clear();
		}
	}

	<T extends Handler> void finishObjectEvents(T handler) throws YamlException  {
		int update=length-1;
		if(update>0) {
			for(int i=0;i<update;i++) {
				entries[length-i-1].finish(true, handler);
			}
		}
		entries[0].finish(true, handler);
	}

	YamlLine parse(Line input) throws IOException {
		YamlLine result=new YamlLine(structure,length+1);
		result.current=input;
		result.entries[0]=entries[0];
		result.length=1;
		int rover=1;
		int lookahead=0;
		int start=0;
		boolean isArray=false;
		for(int i=0;i<input.length+1;i++) {
			int c=i>=input.length?-1:input.content[i];
			switch(c) {
				case ' ':if(i==lookahead) {
						if(isArray) {
							if(input.content[start]==' ') {
								throw new IOException("Shouldn't happen.");
							}
							break;
						}
						start=i;
						if(rover<length) {
							if(rover==length-1&&entries[rover].length==1&&entries[rover].open) {
								isArray=true;
								lookahead=Integer.MAX_VALUE;
							} else {
								lookahead+=entries[rover].length;
							}
							result.set(rover,entries[rover]);
							rover++;
						} else {
							lookahead=Integer.MAX_VALUE;
						}
					}
					break;
				case '-':if(i<input.length-1&&input.content[i+1]!=' ') {
						// Non space, plain style
					} else {
						IndentationEntry parent=result.entries[rover-1];
						if(isArray) {
							// Was array. Create new instances
							parent.fix(i-start);
							lookahead=Integer.MAX_VALUE;
						} else {
							result.firstArray=rover;
						}
						if(lookahead==Integer.MAX_VALUE) {
							if(!isArray) {
								throw new IOException();
							}
							result.set(rover,new IndentationEntry(parent,1,true,true));
							rover++;
						} else if(i<lookahead) {
							// - in between
							throw new YamlException("Bad indentation: An indentation of length "+lookahead+" was expected.");
						} else {
							if(rover<length) {
								if(!entries[rover].array) {
									// Not an array
									throw new IOException();
								}
								result.set(rover,entries[rover].open());
								rover++;
							} else {
								result.set(rover,new IndentationEntry(parent,1,true,true));
								rover++;
							}
							lookahead=Integer.MAX_VALUE;
						}
						start=i;
						isArray=true;
						break;
					}
				    // Fall through and check explicitly
				case '#': if(c=='#') {
						if(!isArray) {
							return null;
						}
						c=-1;
					}
					// Fall through
				default: // Includes -1
						result.delimiter=c>=0?Character.toString((char)c):"";
						if(c<0) {
							if(!isArray) {
								return null;
							}
						}
						if(i==0) {
							return result;
						}
						result.size=i;
						if(isArray) {
							// Already in
							if(c>=0) {
								result.entries[rover-1].fix(i-start);
							}
						} else if(lookahead==Integer.MAX_VALUE) {
							result.set(rover, new IndentationEntry(result.entries[rover-1],i-start,false,false));
							rover++;
						} else if(i<lookahead) {
							throw new YamlException("Bad indentation: An indentation of length "+size+" was expected.");
						}
						result.length=rover;
						return result;
			}				
		}
		// Cannot happen		
		return null;
	}
	
	class FlowEntry {
		final FlowEntry parent;
		final boolean array;
		boolean key0;
		int count=0;
		boolean finished;
		boolean separated=true;
		private FlowEntry(FlowEntry parent,boolean array) {
			this.parent=parent;
			this.array=array;
		}
		
		<T extends Handler> void startObject(List<Token> modifier,T handler) throws YamlException {
			if(parent!=null) {
				parent.incr();
				key0=parent.isKey();
			}
			if(array) {
				handler.beginArray(key0, modifier);
			} else {
				handler.beginObject(key0, modifier);
			}
			modifier.clear();
		}
		
		boolean checkState() throws YamlException {
			if(separated&&count>0) {
				if(!isKey()) {
					throw new YamlException("Misplaced ,");
				} else {
					throw new YamlException("Value expected");
				}
			} else if(isKey()) {
				throw new YamlException("Missing : in flow mode");
			}
			return true;
		}
		
		<T extends Handler> void endObject(boolean a,T handler) throws YamlException {
			if(a!=array) {
				throw new YamlException("Misplaced bracket");
			}
			checkState();
			if(array) {
				handler.endArray(key0);
			} else {
				handler.endObject(key0);				
			}
			level.pop();
		}

		public void handleSeparator() throws YamlException {
			if(separated||isKey()) {
				throw new YamlException("Misplaced separator");
			}
			separated=true;
		}
		
		public void incr() throws YamlException {
			if(!separated) {
				throw new YamlException("Missing , or :");
			}
			count++;
			separated=false;
		}
		
		boolean isKey() {
			return !array&&1==(count&1);
		}
		
		public void handleValueIndicator() throws YamlException {
			if(separated||!isKey()) {
				throw new YamlException("Misplaced :");
			}
			separated=true;
		}
	}

	
	static class IndentationEntry {
		private IndentationEntry parent;
		private final boolean array;
		private int length;
		private boolean open;
		private int lineMode=0;
		private int eventMask;
		private List<Token> objectModifier;
		private List<Token> modifier;
		private Token token;
		private IndentationEntry(IndentationEntry parent,int length,boolean array,boolean open) {
			this.parent=parent;
			this.length=open?1:length;
			this.array=array;
			this.open=open;
		}
		
		private void updateModifiers(List<Token> modifier) {
			if(objectModifier==null) {
				objectModifier=new ArrayList<>(modifier);
			} else {
				objectModifier.addAll(modifier);
			}
		}

		private boolean isKey() {
			return parent!=null&&1==(parent.lineMode&0xf);
		}
		
		private IndentationEntry open() {
			length=1;
			open=true;
			return this;
		}
		private IndentationEntry fix(int l) {
			length=l;
			open=false;
			return this;
		}
		
		<T extends Handler> void key(Mode mode,List<Token> modifier,T handler) throws YamlException {
			if(lineMode>=16||token!=null||mode!=Mode.Indented) {
				throw new YamlException("Misplaced key indicator");
			}
			this.objectModifier=modifier;
			lineMode=1;
			startObject(handler);
			modifier.clear();
		}
		
		<T extends Handler> void value(Mode mode,T handler) throws YamlException {
			if(token!=null) {
				finishToken(true, handler);
			}
			if(lineMode<16||mode!=Mode.Indented) {
				throw new YamlException("Misplaced :");
			}
			lineMode=2;
		}
		
		private void generateMisplacedTokenException() throws YamlException {
			if(array) {
				throw new YamlException("Missing - to indicate new array item.");
			} else {
				throw new YamlException("Misplaced node (two strings?)");
			}
		}
		
		<T extends Handler> void scalar(List<Token> modifier,Token t,T handler) throws YamlException {
			int mode=lineMode&0xf;
			if(mode!=0) {
				this.token=t;
				this.modifier=modifier;
				finishToken(mode==1, handler);
			} else {
				if(this.token!=null) {
					generateMisplacedTokenException();
				}
				this.token=t;
				this.modifier=modifier.size()==0?modifier:new ArrayList<>(modifier);
			}
			modifier.clear();
		}
		
		private <T extends Handler> void startObject(T handler) throws YamlException {
			if(0==(eventMask&2)) {
				if(parent!=null&&parent.token!=null) {
					parent.generateMisplacedTokenException();
				}
				handler.beginObject(isKey(),objectModifier==null?Collections.emptyList():objectModifier);
				objectModifier=null;
				eventMask|=2;
			}
		}
		
		private <T extends Handler> void finishObject(T handler) throws YamlException {
			if(0!=(eventMask&2)) {
				if(token!=null&&2!=(lineMode&0xf)) {
					throw new YamlException("Incomplete line");
				}
				finishToken(false,handler);
				handler.endObject(isKey());
				if(!array&&parent!=null) {
					parent.handleChildObject();
				}
				eventMask&=~2;
			} else {
				finishToken(false,handler);
			}
		}
		
		private void handleChildObject() {
			switch(lineMode&0xf) {
				case 0:token=OBJECT_INDICATOR;
				case 2:lineMode=0;
					break;
				case 1:lineMode=0x10;
					break;
			}
		}
		
		private <T extends Handler> void finishToken(boolean key,T handler) throws YamlException {
			if(token!=null) {
				if(token!=OBJECT_INDICATOR) {
					if(key) {
						startObject(handler);
					} else if(0==(eventMask&2)&&parent!=null&&!array) {
						parent.scalar(modifier, token, handler);
						return;
					}
					if(objectModifier!=null) {
						objectModifier.addAll(modifier);
						modifier=objectModifier;
						objectModifier=null;
					}
					handler.scalar(key,modifier, token);
					lineMode=0;
				}
				lineMode=key?lineMode|0x10:(lineMode&0xf);
				token=null;
				modifier=null;
			}
		}
		
		public <T extends Handler> void finish(boolean complete,T handler) throws YamlException {
			finishObject(handler);
			if(complete&&0!=(eventMask&1)) {
				handler.endArray(isKey());
				eventMask=0;
				parent.handleChildObject();
			}
		}
		
		public <T extends Handler> void start(List<Token> modifier,T handler) throws YamlException {
			if(0==(eventMask&1)&&array) {
				if(parent.token!=null||(0==(parent.lineMode&0xf)&&2==(parent.eventMask&2))) {
					parent.generateMisplacedTokenException();
				}
				handler.beginArray(isKey(),modifier);
				eventMask|=1;
			} else if(modifier.size()>0) {
				this.objectModifier=new ArrayList<>(modifier);
			}
		}
	}
	
	private class Continuation {
		private Chomping chomping;
		private boolean literal;
		private int lowerBound;
		private int offset;
		boolean pendingWhitespace=false;
		private StringBuilder saved=new StringBuilder();
		private StringBuilder value=new StringBuilder();
		private Continuation(int offset,int lowerBound,boolean literal,Chomping chomping) {
			this.chomping=chomping;
			this.lowerBound=lowerBound;
			this.offset=offset;
			this.literal=literal;
		}
		private String getValue() {
			if(chomping==Chomping.Keep) {
				value.append(saved);
			}
			return value.toString();
		}
		private boolean consume(Line input) throws IOException {
			int indent=0;
			while(indent<input.length&&input.content[indent]==' ') {
				indent++;
			}
			if(indent==input.length) {
				saved.append(input.lineBreak);
				pendingWhitespace=false;
				return true;
			}
			if(offset<0) {
				if(indent<lowerBound) {
					return false;
				}
				offset=indent;
			}
			if(indent==offset&&!literal&&input.lineBreak=='\n') {
				saved.setLength(Math.min(saved.length(),1));
				(pendingWhitespace?value.append(' '):value).append(saved).append(input.content,offset,input.length-offset);
				saved.setLength(0);
				pendingWhitespace=true;
				return true;
			} else if(indent>=offset) {
				saved.setLength(Math.min(saved.length(),1));
				(pendingWhitespace?value.append('\n'):value).append(saved).append(input.content,offset,input.length-offset);
				(literal||input.lineBreak!='\n'?value:saved).append(input.lineBreak);
				pendingWhitespace=false;
				saved.setLength(0);
				return true;
			} else {
				if(chomping==Chomping.Strip&&value.length()>0&&value.charAt(value.length()-1)=='\n') {
					value.setLength(value.length()-1);
				}
				return false;
			}
		}
		private boolean finish() {
			return true;
		}
	}
	
	interface Tokenizer extends Iterator<Token> {
		public boolean isContinued();
		public void update(Line line) throws IOException;
		public void eof() throws IOException;
		public YamlLine nextLine() throws IOException;
		public default boolean isFlowMode() {
			return false;
		}
		public default FlowEntry peekFlowEntry() {
			throw new RuntimeException("Not in flow mode");
		}
		public default IndentationEntry peekIndentationEntry() {
			throw new RuntimeException("Not in flow mode");
		}
	}
	
	private enum Chomping {
		Strip,Clip,Keep;
	}
	
	class DefaultTokenizer implements Tokenizer {
		protected int offset;
		protected int mode=-1;
		protected Token next;
		protected String prefix;
		protected StringBuilder currentToken=new StringBuilder();
		protected Continuation continuation;

		private DefaultTokenizer() {
			offset=size;
		}
		
		@Override
		public boolean isFlowMode() {
			return YamlLine.this.isFlowMode();
		}

		@Override
		public FlowEntry peekFlowEntry() {
			return level.peek();
		}

		@Override
		public IndentationEntry peekIndentationEntry() {
			return entries[length-1];
		}

		@Override
		public boolean hasNext() {
			if(mode<0) {
				lookAhead();
			}
			return mode==1;
		}

		@Override
		public Token next() {
			try {
			return next;
			} finally {
				mode=-1;
			}
		}

		@Override
		public boolean isContinued() {
			if(mode<0) {
				lookAhead();
			}
			return mode>1||isFlowMode();
		}
		
		private boolean isNextspace() {
			return offset>=current.length-1||current.content[offset]==' ';
		}
		
		private Type getType(int syntaxMode) {
			switch(syntaxMode) {
				case 3:return Type.Anchor;
				case 4:return Type.Alias;
				case 5:return Type.Script;
				default:return Type.Scalar;
			}
		}
		
		private boolean isWhiteSpace(char c) {
			return c==' '||c=='\t';
		}
		
		private void parseSingleQuotedString(boolean inner) {
			int lastNonSpace=-1;
			while(offset<current.length) {
				char c=current.content[offset];
				if(c=='\'') {
					offset++;
					if(offset<current.length&&current.content[offset]=='\'') {
					} else {
						if(inner&&lastNonSpace<0&&currentToken.length()>0) {
							stripTrailingWhiteSpace();
						}
						next=new SimpleToken(Type.Scalar,prefix==null?"!str":prefix,currentToken.toString());
						mode=1;
						return;
					}
				} else if(isWhiteSpace(c)) {
					if(inner&&lastNonSpace<0) {
						offset++;
						continue;
					}
				} else {
					lastNonSpace=currentToken.length();
				}
				currentToken.append(c);
				offset++;
			}
			if(lastNonSpace>=0) {
				currentToken.setLength(lastNonSpace+1);
				currentToken.append(current.lineBreak=='\n'?' ':current.lineBreak);
			} else if(inner) {
				stripTrailingWhiteSpace();
				currentToken.append(current.lineBreak);
			}
			mode=2;			
		}
		
		private void stripTrailingWhiteSpace() {
			while(currentToken.length()>0&&isWhiteSpace(currentToken.charAt(currentToken.length()-1))) {
				currentToken.setLength(currentToken.length()-1);
			}
		}
		
		private void parseDoubleQuotedString(boolean inner,int currentMode) {
			boolean spaced=false;
			if(inner&&currentMode<128) {
				stripTrailingWhiteSpace();
				currentToken.append(' ');
				spaced=true;
			}
			mode=3;
			int lastNonSpace=-1;
			while(offset<current.length) {
				if(current.content[offset]=='\\') {
					offset+=2;
					lastNonSpace=currentToken.length()+1;
					if(offset>current.length) {
						mode+=128;
						return;
					} else switch(current.content[offset-1]) {
						case '\\':currentToken.append('\\');
							continue;
						case '"':currentToken.append('"');
							continue;
						case 'a':currentToken.append((char)0x07);
							continue;
						case 'b':currentToken.append((char)0x08);
							continue;
						case 'e':currentToken.append((char)0x1b);
							continue;
						case 'f':currentToken.append((char)0x0c);
							continue;
						case 'r':currentToken.append('\r');
							continue;
						case 'n':currentToken.append('\n');
							continue;
						case 't':currentToken.append('\t');
							continue;
						case 'v':currentToken.append((char)0x0b);
							continue;
						case '0':currentToken.append((char)0x00);
							continue;
						case ' ':currentToken.append(' ');
							continue;
						case '_':currentToken.append((char)0xa0);
							continue;
						case 'N':currentToken.append((char)0x85);
							continue;
						case 'L':currentToken.append((char)0x2028);
							continue;
						case 'P':currentToken.append((char)0x2029);
							continue;
						case 'x':if(offset+1<current.length) try {
								currentToken.append((char)Integer.parseInt(new String(current.content,offset,2),16));
								offset+=2;
								continue;
							} catch(Throwable t) {
							}
							throw new InternalException("Two hex digits expected after \\x");
						case 'u':if(offset+3<current.length) try {
								currentToken.append((char)(Integer.parseInt(new String(current.content,offset,4),16)&0xffff));
								offset+=4;
								continue;
							} catch(Throwable t) {
							}
							throw new InternalException("Four hex digits expected after \\u");
						case 'U':if(offset+7<current.length) try {
								currentToken.append((char)(Integer.parseInt(new String(current.content,offset,8),16)&0xffff));
								offset+=8;
								continue;
							} catch(Throwable t) {
							}
							throw new InternalException("Eight hex digits expected after \\U");
						default: throw new InternalException("Illegal character after \\: "+current.content[offset-1]);
					}
				} else if(current.content[offset]=='"') {
					offset++;
					if(spaced) {
						currentToken.setLength(currentToken.length()-1);
					}
					next=new SimpleToken(Type.Scalar,prefix==null?"!str":prefix,currentToken.toString());
					mode=1;
					return;
				} else if(isWhiteSpace(current.content[offset])) {
					if(inner&&lastNonSpace<0) {
						offset++;
						continue;
					}
				} else {
					lastNonSpace=current.length;
				}
				inner=false;
				currentToken.append(current.content[offset++]);
				lastNonSpace=currentToken.length();
			}
			if(lastNonSpace<0) {
				if(spaced) {
					// Empty line
					currentToken.setLength(currentToken.length()-1);
				}
				currentToken.append(current.lineBreak);
			} else if(current.lineBreak!='\n') {
				stripTrailingWhiteSpace();
				currentToken.append(current.lineBreak);
				mode+=128;
			}
		}
		
		private String parsePrefix() {
			StringBuilder prefix=new StringBuilder();
			while(offset<current.length) {
				if(current.content[offset]!=' ') {
					offset++;
				} else {
					break;
				}
			}
			this.prefix=prefix.toString();
			skipWhiteSpace();
			return this.prefix;
		}
		
		private void lookAhead() {
			skipWhiteSpace();
			if(mode<0) {
				int syntaxMode=0;
				currentToken.setLength(0);
				prefix=null;
				Token next=null;
				if(isDocumentStart()&&current.content[offset]!='&') {
					mode=0;
					return;
				}
				outerloop: while(offset<current.length) {
					char c=current.content[offset];
					offset++;
					switch(c) {
						case '>':
						case '|':if(syntaxMode==0) {
								int lowerBound=size;
								if(length>0&&entries[length-1].array) {
									lowerBound-=entries[length-1].length;
								}
								Chomping chomping=Chomping.Clip;
								int blockOffset=-1;
								while(offset<current.length) {
									switch(current.content[offset]) {
									case '1':
									case '2':
									case '3':
									case '4':
									case '5':
									case '6':
									case '7':
									case '8':
									case '9':blockOffset=lowerBound+(current.content[offset])-'0';
										break;
									case '+':chomping=Chomping.Keep;
										break;
									case '-':chomping=Chomping.Strip;
										break;
									}
									offset++;
								}
								continuation=new Continuation(blockOffset,lowerBound+1,c=='|',chomping);
								mode=4;
								return;
							}
							break;
						case '!':if(syntaxMode==0) {
								prefix=parsePrefix();
							}
							continue outerloop;
						case '#':if(offset==1||currentToken.length()==0) {
								mode=0;
								offset=current.length;
								return;
							} else if(current.content[offset-2]==' ') {
								offset-=2;
								break outerloop;
							}
							break;
						case '\'': if(syntaxMode==0) {
								parseSingleQuotedString(false);
								return;
							}
							break;
						case '\"': if(syntaxMode==0) {
								parseDoubleQuotedString(false,3);
								return;
							}
							break;
						case '?':
						case ':': if(isNextspace()||isFlowMode()) {
								if(syntaxMode==0) {
									next=c=='?'?KEY_INDICATOR:VALUE_INDICATOR;
									break outerloop;
								} else {
									offset--;
									next=new SimpleToken(getType(syntaxMode),prefix,currentToken.toString());
									break outerloop;
								}
							}
							break;
						case '&':if(syntaxMode==0) {
								syntaxMode=3;
								continue outerloop;
							}
							break;
						case '*':if(syntaxMode==0) {
								syntaxMode=4;
								continue outerloop;
							}
							break;
						case '@':if(syntaxMode==0&&structure.supportsScripts()) {
								syntaxMode=5;
								continue outerloop;
							}
							break;
						case ' ':if(syntaxMode==3||syntaxMode==4||syntaxMode==5) {
								next=new SimpleToken(getType(syntaxMode),prefix,currentToken.toString());
								break outerloop;
							}
							break;
						case '[':if(syntaxMode==0) {
								next=SQUARE_OPEN;
								addLevel(new FlowEntry(currentLevel(),true));
							} else if(isFlowMode()) {
								offset--;
							} else {
								break;
							}
							break outerloop;
						case '{':if(syntaxMode==0) {
								next=CURLY_OPEN;
								addLevel(new FlowEntry(currentLevel(),false));
							} else if(isFlowMode()) {
								offset--;
							} else {
								break;
							}
							break outerloop;
						case '}':if(isFlowMode()) {
								if(syntaxMode==0) {
									next=CURLY_CLOSE;
								} else {
									offset--;
								}
								break outerloop;
							}
							break;
						case ']':if(isFlowMode()) {
								if(syntaxMode==0) {
									next=SQUARE_CLOSE;
								} else {
									offset--;
								}
								break outerloop;
							}
							break;
						case ',':if(isFlowMode()) {
								if(syntaxMode==0) {
									next=SEPARATOR;
								} else {
									offset--;
								}
								break outerloop;
							}
							break;
						default:if(syntaxMode==0) {
							syntaxMode=6;
						}
					}
					currentToken.append(c);
				}
				// finished
				switch(syntaxMode) {
					case 3:
					case 4:
					case 5:
					default: if(next==null) {
						next=new SimpleToken(getType(syntaxMode),prefix,currentToken.toString().trim());
				}
				this.next=next;
				this.mode=1;
				}
			}
		}
		
		private void skipWhiteSpace() {
			while(offset<current.length&&current.content[offset]==' ') {
				offset++;
			}
			if(offset>=current.length) {
				mode=0;
			}
		}

		@Override
		public void update(Line line) throws IOException {
			current=line;
			int savedMode=mode;
			mode=-1;
			offset=0;
			if(savedMode==2) {
				parseSingleQuotedString(true);
			} else if((savedMode&0xf)==3) {
				parseDoubleQuotedString(true,savedMode);
			} else if(savedMode==4) {
				if(!continuation.consume(line)) {
					next=new SimpleToken(Type.Scalar,prefix==null?"!str":prefix,continuation.getValue());
					mode=1;
					offset=current.length;
				} else {
					mode=savedMode;
				}
			}
		}

		@Override
		public YamlLine nextLine() throws IOException {
			if(continuation!=null&&mode==0) {
				return structure.parse(current,true);
			}
			return null;
		}

		@Override
		public void eof() throws IOException {
			if(mode==4) {
				if(continuation.finish()) {
					next=new SimpleToken(Type.Scalar,prefix,continuation.getValue());
					mode=1;
					offset=current.length;
				}
			}
			
		}
	}
	
	private static class InternalToken implements Token {
		Type type;
		String val;
		private InternalToken(Type type,String val) {
			this.type=type;
			this.val=val;
		}
		@Override
		public Type getType() {
			return type;
		}
		@Override
		public String getValue() {
			return val;
		}
		
		@Override
		public String toString() {
			return getValue();
		}
	}
}
