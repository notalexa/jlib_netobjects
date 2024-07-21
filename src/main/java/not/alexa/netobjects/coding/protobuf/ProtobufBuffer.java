/*
 * Copyright (C) 2024 Not Alexa
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
package not.alexa.netobjects.coding.protobuf;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Buffer class in the protobuf framework.
 * 
 * @author notalexa
 */
class ProtobufBuffer {
	private static Charset UTF8=Charset.forName("UTF-8");
	private Entry currentEntry;
	private Entry buffered;
	private byte[] currentBuffer;
	private int offset;
	private int len;
	private int[] bufferLength;
	private byte[][] bufferBytes;
	private int used=-1;
	private int tagLength;

	public ProtobufBuffer() {
		this(new byte[2048]);
	}
	public ProtobufBuffer(byte[] buffer) {
		this(buffer,0,buffer.length);
	}

	public ProtobufBuffer(byte[] buffer,int offset,int len) {
		this.currentBuffer=buffer;
		this.offset=offset;
		this.len=offset+len;
	}
	
	public void consume(ProtobufListener listener) {
		try {
			while(offset<len) {
				int tag=readInt();
				switch(tag&0x7) {
				case 0: listener.consume(tag>>3, readLong());
					break;
				case 1: listener.consume(tag>>3, readFixed64());
					break;
				case 5: listener.consume(tag>>3, readFixed32());
					break;
				case 2: int len=readInt();
					listener.consume(tag>>3, currentBuffer, offset,len);
					offset+=len;
					break;
				default: throw new RuntimeException();
				}
			}
			listener.done();
		} catch(IOException e) {
			listener.onError(offset,e);
		}
	}
	
	public int readFixed32() {
		int ret=((currentBuffer[offset+3]&0xff)<<24)
				|((currentBuffer[offset+2]&0xff)<<16)
				|((currentBuffer[offset+1]&0xff)<<8)
				|((currentBuffer[offset]&0xff));
		offset+=4;
		return ret;
	}

	public long readFixed64() {
		long ret=
				((currentBuffer[offset+7]&0xffL)<<56)
				|((currentBuffer[offset+6]&0xffL)<<48)
				|((currentBuffer[offset+5]&0xffL)<<40)
				|((currentBuffer[offset+4]&0xffL)<<32)
				|((currentBuffer[offset+3]&0xffL)<<24)
				|((currentBuffer[offset+2]&0xffL)<<16)
				|((currentBuffer[offset+1]&0xffL)<<8)
				|((currentBuffer[offset]&0xffL));
		offset+=8;
		return ret;
	}

	public int readInt() throws IOException {
		int accu=0;
		int shift=0;
		while(offset<currentBuffer.length) {
			byte b=currentBuffer[offset++];
			accu|=((b&0x7f)<<shift);
			if(b>=0) {
				return accu;
			}
			shift+=7;
		}
		throw new EOFException();
	}
	
	public void ensureLength(int len) {
		if(currentBuffer.length<offset+len) {
			if(offset<2048) {
				currentBuffer=Arrays.copyOf(currentBuffer, len+2048);
				return;
			}
			if(used<0) {
				bufferLength=new int[50];
				bufferBytes=new byte[50][];
			} else if(used==bufferLength.length) {
				bufferLength=Arrays.copyOf(bufferLength, bufferLength.length+50);
				bufferBytes=Arrays.copyOf(bufferBytes, bufferBytes.length+50);
			}
			used++;
			bufferBytes[used]=currentBuffer;
			bufferLength[used]=offset;
			Entry rover=currentEntry;
			while(rover!=null) {
				if(rover.index<0) {
					rover.index=used;
					rover=rover.prev;
				} else {
					rover=null;
				}
			}
			currentBuffer=new byte[Math.max(8192, len+256)];
			offset=0;
		}
	}
	
	private int writeLong(byte[] currentBuffer,int offset,long l) {
		if(l<0) {
			currentBuffer[offset+9]=(byte)(0x00|((l>>(63)&0x1)));
			currentBuffer[offset+8]=(byte)(0x80|((l>>(56)&0x7f)));
			currentBuffer[offset+7]=(byte)(0x80|((l>>(49)&0x7f)));
			currentBuffer[offset+6]=(byte)(0x80|((l>>(42)&0x7f)));
			currentBuffer[offset+5]=(byte)(0x80|((l>>(35)&0x7f)));
			currentBuffer[offset+4]=(byte)(0x80|((l>>(28)&0x7f)));
			currentBuffer[offset+3]=(byte)(0x80|((l>>(21)&0x7f)));
			currentBuffer[offset+2]=(byte)(0x80|((l>>(14)&0x7f)));
			currentBuffer[offset+1]=(byte)(0x80|((l>>(7)&0x7f)));
			currentBuffer[offset+0]=(byte)(0x80|((l>>(0)&0x7f)));
			offset+=10;
		} else if(l<(1<<7)) {
			currentBuffer[offset++]=(byte)(l&0x7f);
		} else if(l<(1<<14)) {
			currentBuffer[offset+1]=(byte)((l>>7)&0x7f);
			currentBuffer[offset]=(byte)(0x80|(l&0x7f));
			offset+=2;
		} else if(l<(1<<21)) {
			currentBuffer[offset+2]=(byte)((l>>14)&0x7f);
			currentBuffer[offset+1]=(byte)(0x80|((l>>7)&0x7f));
			currentBuffer[offset]=(byte)(0x80|(l&0x7f));
			offset+=3;
		} else {
			currentBuffer[offset+2]=(byte)(0x80|((l>>14)&0x7f));
			currentBuffer[offset+1]=(byte)(0x80|((l>>7)&0x7f));
			currentBuffer[offset]=(byte)(0x80|(l&0x7f));
			offset=writeLong(currentBuffer,offset+3,l>>21);
		}
		return offset;
	}

	private int writeInt(byte[] currentBuffer,int offset,int i) {
		if(i<0) {
			return writeLong(currentBuffer,offset,i);
		} else if(i<(1<<7)) {
			currentBuffer[offset++]=(byte)(i&0x7f);
		} else if(i<(1<<14)) {
			currentBuffer[offset+1]=(byte)((i>>7)&0x7f);
			currentBuffer[offset]=(byte)(0x80|(i&0x7f));
			offset+=2;
		} else if(i<(1<<21)) {
			currentBuffer[offset+2]=(byte)((i>>14)&0x7f);
			currentBuffer[offset+1]=(byte)(0x80|((i>>7)&0x7f));
			currentBuffer[offset]=(byte)(0x80|(i&0x7f));
			offset+=3;
		} else {
			currentBuffer[offset+2]=(byte)(0x80|((i>>14)&0x7f));
			currentBuffer[offset+1]=(byte)(0x80|((i>>7)&0x7f));
			currentBuffer[offset]=(byte)(0x80|(i&0x7f));
			offset=writeInt(currentBuffer,offset+3,i>>21);
		}
		return offset;
	}
	
	public <T extends OutputStream> T writeTo(T out) throws IOException {
		if(used>=0) for(int i=0;i<=used;i++){
			out.write(bufferBytes[i],0,bufferLength[i]);
		}
		out.write(currentBuffer,0,offset);
		return out;
	}
	
	public void close() {
		if(offset<currentBuffer.length) {
			currentBuffer=Arrays.copyOf(currentBuffer, offset);
			offset=0;
		}
	}
	
	public ProtobufBuffer push(int field) {
		ensureLength(20);
		byte[] buffer=currentBuffer;
		int c0=offset;
		offset=writeInt(buffer,offset,(field<<3)|2);
		tagLength+=offset-c0;
		if(buffered!=null) {
			Entry c=buffered.prev;
			buffered.prev=currentEntry;
			buffered.offset=offset;
			currentEntry=buffered;
			buffered=c;
		} else {
			currentEntry=new Entry(currentEntry,offset);
		}
		currentEntry.index=-1;
		currentEntry.reserved=1;
		currentEntry.len=tagLength;
		tagLength=0;
		// Minimum of 1 byte
		offset++;
		return this;
	}
	
	int encodingLength(int i) {
		if(i<128) {
			return 1;
		} else if(i<(1<<14)) {
			return 2;
		} else if(i<(1<<21)) {
			return 3;
		} else {
			return 3+encodingLength(i>>21);
		}
	}
	
	void shift(byte[] buffer,int start,int end,int shift) {
		for(int i=end-1;i>=start;i--) {
			buffer[i+shift]=buffer[i];
		}
	}
	
	public ProtobufBuffer pop() {
		if(currentEntry!=null) {
			int popOffset=currentEntry.index>=0?bufferLength[currentEntry.index]:offset;
			byte[] popBuffer=currentEntry.index>=0?bufferBytes[currentEntry.index]:currentBuffer;
			if(tagLength<128) {
				popBuffer[currentEntry.offset]=(byte)tagLength;
				tagLength++;
			} else {
				int l=encodingLength(tagLength);
				if(currentEntry.reserved<l) {
					// Calculate length of parent with current payload added
					int parentShift=currentEntry.calculateParentShift(l+tagLength);
					int shift=l-currentEntry.reserved+parentShift;
					shift(popBuffer,currentEntry.offset,popOffset,shift);
					if(parentShift>0) {
						currentEntry.prev.shift(popBuffer,currentEntry.offset,l+tagLength,parentShift);
						currentEntry.offset+=parentShift;
					}
					if(currentEntry.index>=0) {
						bufferLength[currentEntry.index]+=shift;
					} else {
						offset+=shift;
					}
				}
				writeInt(popBuffer,currentEntry.offset,tagLength);
				tagLength+=l;
			}
			tagLength+=currentEntry.len;
			Entry released=currentEntry;
			currentEntry=currentEntry.prev;
			released.prev=buffered;
			buffered=released;
		}
		return this;
	}

	public ProtobufBuffer write(int field,int l) {
		ensureLength(20);
		int c=offset;
		byte[] buffer=currentBuffer;
		offset=writeInt(buffer,offset,(field<<3));
		offset=writeInt(buffer,offset,l);
		tagLength+=(offset-c);
		return this;
	}
	
	public static long gazGiz(long l) {
		if(0!=(l&1)) {
			return -(l>>>1)-1;
		} else  {
			return (l>>>1);
		}
	}

	public long readZigZag() throws IOException {
		return gazGiz(readLong());
	}

	public ProtobufBuffer writeZigZag(int field,long l) {
		ensureLength(20);
		if(l<0) {
			l=(-l<<1)-1;
		} else {
			l=l<<1;
		}
		byte[] buffer=currentBuffer;
		int c=offset;
		offset=writeInt(buffer,offset,(field<<3));
		offset=writeLong(buffer,offset,l);
		tagLength+=(offset-c);
		return this;
	}
	
	public ProtobufBuffer writeFixedInt(int field,int i) {
		ensureLength(18);
		int c=offset;
		byte[] buffer=currentBuffer;
		offset=writeInt(buffer,offset,(field<<3)|5);
		buffer[offset]=(byte)(0xff&(i>>0));
		buffer[offset+1]=(byte)(0xff&(i>>8));
		buffer[offset+2]=(byte)(0xff&(i>>16));
		buffer[offset+3]=(byte)(0xff&(i>>24));
		offset+=4;
		tagLength+=(offset-c);
		return this;
	}

	public ProtobufBuffer writeFixedLong(int field,long l) {
		ensureLength(18);
		int c=offset;
		byte[] buffer=currentBuffer;
		offset=writeInt(buffer,offset,(field<<3)|1);
		buffer[offset]=(byte)(0xff&(l>>0));
		buffer[offset+1]=(byte)(0xff&(l>>8));
		buffer[offset+2]=(byte)(0xff&(l>>16));
		buffer[offset+3]=(byte)(0xff&(l>>24));
		buffer[offset+4]=(byte)(0xff&(l>>32));
		buffer[offset+5]=(byte)(0xff&(l>>40));
		buffer[offset+6]=(byte)(0xff&(l>>48));
		buffer[offset+7]=(byte)(0xff&(l>>56));
		offset+=8;
		tagLength+=(offset-c);
		return this;
	}

	public ProtobufBuffer write(int field,float f) {
		return writeFixedInt(field,Float.floatToIntBits(f));
	}

	public ProtobufBuffer write(int field,double d) {
		return writeFixedLong(field,Double.doubleToLongBits(d));
	}

	public ProtobufBuffer write(int field,long l) {
		ensureLength(20);
		int c=offset;
		byte[] buffer=currentBuffer;
		offset=writeInt(buffer,offset,(field<<3));
		offset=writeLong(buffer,offset,l);
		tagLength+=offset-c;
		return this;
	}

	public ProtobufBuffer write(int field,byte[] s) {
		return write(field,s,0,s.length);
	}
	
	public ProtobufBuffer write(int field,byte[] s,int o,int len) {
		ensureLength(20+len);
		int c=offset;
		byte[] buffer=currentBuffer;
		if(field>0) {
			offset=writeInt(buffer,offset,(field<<3)|2);
			offset=writeInt(buffer,offset,len);
		}
		System.arraycopy(s,o,buffer, offset, len);
		offset+=len;
		tagLength+=offset-c;
		return this;
	}

	public ProtobufBuffer write(int field,String s) {
		return write(field,s.getBytes(UTF8));
	}

	public long readLong() throws IOException {
		long accu=0;
		int shift=0;
		while(offset<currentBuffer.length) {
			byte b=currentBuffer[offset++];
			accu|=((b&0x7f)<<shift);
			if(b>=0) {
				return accu;
			}
			shift+=7;
		}
		throw new EOFException();
	}

	public interface ProtobufListener {
		public default void consume(int field,int value) {}
		public default void consume(int field,long value) {}
		public default void consume(int field,byte[] value,int offset,int len) {}
		public default void onError(int offset,IOException e) {}
		public default void done() {}
	}
	
	private class Entry {
		Entry prev;
		int offset;
		int index=-1;
		int reserved;
		private int len;
		private Entry(Entry prev,int offset) {
			this.offset=offset;
			this.prev=prev;
		}
		public void shift(byte[] buffer,int offset, int total,int shift) {
			int l=total+offset-this.offset-reserved;
			int encodedLength=encodingLength(l);
			if(encodedLength>reserved) {
				for(int i=offset-1;i>=this.offset;i--) {
					buffer[i+shift]=buffer[i];
				}
				shift-=(encodedLength-reserved);
				if(shift>0) {
					prev.shift(buffer, this.offset, total+encodedLength+offset-this.offset, shift);
				}
				this.offset+=shift;
				// Need to adjust.
				reserved=encodedLength;
			}
		}
		
		public int calculateParentShift(int l) {
			if(prev!=null&&prev.index==index) {
				l+=offset-prev.offset;
				int encodedLength=encodingLength(l);
				if(encodedLength==prev.reserved) {
					return 0;
				} else {
					return encodedLength-prev.reserved+prev.calculateParentShift(encodedLength+l);
				}
			}
			return 0;
		}
	}

}
