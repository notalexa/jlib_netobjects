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
package not.alexa.netobjects.coding.text;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * An alternative reader handling character streams. Typically, a {@link Line} and a reader are created and the content is read in loop:
 * <pre>
 *   Line line=new Line();
 *   try(LineReader reader=new LineReader(stream)) {
 *     while(reader.readLine(line)) {
 *       ...
 *     }
 *   }
 * </pre>
 * Every call to {@link #readLine(Line)} overwrites the content of the line. The reader provides basic location information in the stream, which can be 
 * set using the {@link LineAware} interface.
 * <p>For custom handling of line breaks, see {@link #lineBreak(Line, char)}.
 *   
 * @author notalexa
 *
 */
public class LineReader implements Closeable {
	/**
	 * Convenience declaration of the most common eight bit encoding
	 */
	public static final Charset ISO_8859_1=Charset.forName("ISO-8859-1");
	/**
	 * Convenience declaration of the most common unicode encoding
	 */
	public static final Charset UTF8=Charset.forName("UTF-8");
	
	boolean normalizeCRLF;
	boolean skipLf;
	int lineNo;
	int charPos;
	final Reader reader;
	

	/**
	 * Create a line reader evaluating the BOM if present and use UTF-8 as the default encoding.
	 * @param stream the input stream
	 */
	public LineReader(InputStream stream) {
		this(createReader(stream,UTF8));
	}

	/**
	 * Create a line reader from the provided reader with normalized carriage return/line feed.
	 * 
	 * @param reader the input reader
	 */
	public LineReader(Reader reader) {
		this(reader,true);
	}

	/**
	 * Create a line reader from the provided reader. Carriage return/line feed are normalized <b>as a carriage return</b> if the
	 * flag {@code normlizeCRLF} flag is set. Otherwise, both characters are kept in the stream. 
	 * 
	 * 
	 * @param reader the input reader
	 * @param normalizeCRLF if {@code true}, carriage return/line feeds are normalized
	 */
	public LineReader(Reader reader,boolean normalizeCRLF) {
		this.reader=reader;
		this.normalizeCRLF=normalizeCRLF;
	}

	/**
	 * Create a reader based on the BOM if available or the default character set if not present.
	 * 
	 * @param stream the input stream
	 * @param defaultCharset the default character set
	 * @return a reader with the default character set if no BOM is available and the character set defined by the BOM otherwise
	 */
	public static Reader createReader(InputStream stream,Charset defaultCharset) {
		if(!stream.markSupported()) {
			stream=new BufferedInputStream(stream);
		}
		stream.mark(5);
		try {
			int c=stream.read();
			if(c==0xfe) {
				if(0xff==stream.read()) {
					defaultCharset=Charset.forName("UTF-16BE");
				} else {
					stream.reset();
				}
			} else if(c==0xff) {
				if(0xfe==stream.read()) {
					stream.mark(3);
					if(0==stream.read()&&0==stream.read()) {
						defaultCharset=Charset.forName("UTF-32LE");
					} else {
						defaultCharset=Charset.forName("UTF-16LE");
						stream.reset();
					}
				} else {
					stream.reset();
				}
				
			} else if(0==c) {
				if(0==stream.read()&&0xfe==stream.read()&&0xff==stream.read()) {
					defaultCharset=Charset.forName("UTF-32BE");
				} else {
					stream.reset();
				}
			} else if(c==0xef) {
				if(0xbb==stream.read()&&0xbf==stream.read()) {
					defaultCharset=UTF8;
				} else {
					stream.reset();
				}
			} else {
				stream.reset();
			}
			return new InputStreamReader(stream,defaultCharset);
		} catch(IOException e) {
			return new InputStreamReader(new InputStream() {
				@Override
				public int read() throws IOException {
					throw e;
				}
			});
		}
	}
	
	/**
	 * Reset the values of the informational counters
	 */
	public void resetCounters() {
		lineNo=charPos=0;
	}
	
	/**
	 * The method decides f the character is a line break character and set the proper value. 
	 * The implementation follows YAML convention. {@code \n}, {@code \r} and {@code 0x85} are normalized to {@code \n}.
	 * {@code 0x2028} and {@code 0x2029} are explicit line breaks and reflected in the line.
	 * <br>This method can be overridden by extensions.
	 * 
	 * @param line the current line
	 * @param c the current character
	 * @return {@code true} if the current character represents a line break
	 * @see {@link #handleMissingLineBreak(Line)} for handling missing line breaks at the end of the stream.
	 */
	protected boolean lineBreak(Line line,char c) {
		switch(c) {
			case '\n':
			case '\r':
			case 0x85: line.lineBreak='\n';
				return true;
			case 0x2028:
			case 0x2029:line.lineBreak=c;
				return true;
			default: return false;
		}
	}
	
	
	/**
	 * This method  is called whenever the last line of the stream is not ended with a line break. The default implementation inserts
	 * a {@code \n} as the line break character into the line.
	 * @param line the current line
	 */
	protected void handleMissingLineBreak(Line line) {
		line.lineBreak='\n';
	}
	
	/**
	 * Read a line from the stream. Previous content of the line will be overwritten.
	 * 
	 * @param line the line to store the content
	 * @return {@¢ode true} if content exists, {@code false} otherwise
	 * @throws IOException if an error occurs on the underlying stream
	 */
	public boolean readLine(Line line) throws IOException {
		line.length=0;
		if(line.content.length>2048) {
			line.content=new char[2048];
		}
		int currentCharPos=charPos;
		boolean lineBreakSeen=false;
		boolean skip=skipLf;
		int v;
		while((v=reader.read())>=0) {
			char c=(char)v;
			currentCharPos++;
			if(c=='\n'&&skip) {
				// Fix the character position
				charPos=currentCharPos;
				continue;
			}
			skip=false;
			if(!lineBreak(line, c)) {
				if(line.length==line.content.length) {
					line.content=Arrays.copyOf(line.content,line.length+2048);
				}
				line.content[line.length]=c;
				line.length++;
			} else {
				if(normalizeCRLF&&c=='\r') {
					skipLf=true;
				}
				lineBreakSeen=true;
				break;
			}
		}
		if(line.length>0||lineBreakSeen) {
			line.lineNo=lineNo;
			line.linePos=0;
			line.charPos=charPos;
			if(!lineBreakSeen) {
				handleMissingLineBreak(line);
			}
			charPos=currentCharPos;
			lineNo++;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}
	
	/**
	 * Data object for the content of a line. All fields are public but should be set in a consistent way. That is:
	 * {@link #content} should not be set to null and {@link #length} should always be &leq; {@code content.length}.

	 * @author notalexa
	 *
	 */
	public static class Line {

		/**
		 * the content of this line (without line break). This value should never be {@code null}.
		 */
		public char[] content;

		/**
		 * the length of the content. This value should always be not negative and &leq; {@code content.length}.
		 * 
		 */
		public int length;
		
		/**
		 * The line break character of the current line.
		 * @see LineReader#lineBreak(Line, char)
		 */
		public char lineBreak='\n';
		
		/**
		 * The current line in the stream starting with 0. For information only.
		 */
		public int lineNo;
		
		/**
		 * The character position of the start of this line in the stream starting with 0. For information only.
		 */
		public int charPos;
		
		/**
		 * The character position in the line starting with 0. This is set to zero while reading and can be changed by the application. For information only.
		 * 
		 */
		public int linePos;
	
		/**
		 * Create an empty line.
		 */
		public Line() {
			content=new char[2048];
		}
		
		/**
		 * Create a line with the given values.
		 * 
		 * @param s the content of the line
		 * @param lineBreak the line break character of the line
		 */
		public Line(String s,char lineBreak) {
			content=s.toCharArray();
			this.length=content.length;
			this.lineBreak=lineBreak;
		}
		
		/**
		 * 
		 * @return the content of the line as a string
		 */
		public String getLine() {
			return content==null?"":new String(content,0,length);
		}
		
		/**
		 * Set the informational values (character position, line number, line position) of this line.
		 * 
		 * @param <T> the type of line aware object
		 * @param t the line aware object
		 * @return the line aware object
		 */
		public <T extends LineAware> T fill(T t) {
			t.setLine(charPos,lineNo,linePos);
			return t;
		}
		
		/**
		 * Set the informational values of this line and throws the exception.
		 * 
		 * @param <R> the return type of this method (this is necessary for constructions like {@code return line.fillAndThrows(...)} to emphasize the 
		 * that this method returns (in the sense that no code will be evaluated after this method was called).
		 * @param <T> the type of the exception
		 * @param t the exception
		 * @return <b>never</b>
		 * @throws T <b>always</b>
		 */
		public <R,T extends Exception&LineAware> R fillAndThrow(T t) throws T {
			throw fill(t);
		}
	}
	
	/**
	 * Interface to mark an object as line aware.
	 * 
	 * @author notalexa
	 * 
	 * @see Line#fill(LineAware)
	 * @see Line#fillAndThrow(Exception)
	 *
	 */
	public interface LineAware {
		
		/**
		 * Set the actual values.
		 * 
		 * @param charPos the current character position of the beginning of the line inside the stream
		 * @param lineNo the current line number inside the stream
		 * @param linePos the current position inside the line
		 */
		public void setLine(int charPos,int lineNo,int linePos);
	}
}

