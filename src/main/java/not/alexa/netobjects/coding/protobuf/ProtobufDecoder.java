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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.DefaultCodingSupport;
import not.alexa.netobjects.coding.protobuf.ProtobufBuffer.ProtobufListener;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.PrimitiveTypeCodec;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.Constructor;

/**
 * Decoder class of the {@link ProtobufCodingScheme}
 * 
 * @author notalexa
 */
class ProtobufDecoder extends DefaultCodingSupport implements Decoder {
	private static final int[] MASK=new int[32];
	static {
		for(int i=0;i<32;i++) {
			MASK[i]=~(1<<i);
		}
	}
	private boolean read;
	private final InputStream stream;
	private final ProtobufCodingScheme scheme;
	private final Context context;

	ProtobufDecoder(Context context,ProtobufCodingScheme scheme,InputStream stream) {
		this.stream=stream;
		this.scheme=scheme;
		this.context=context;
	}

	@Override
	public void close() throws BaseException {
		try {
			stream.close();
		} catch(IOException e) {
			BaseException.throwException(e);
		}
	}
	
	ProtobufBuffer getBuffer() throws BaseException {
		if(!read) try(ByteArrayOutputStream out=new ByteArrayOutputStream()) {
			read=true;
			byte[] buffer=new byte[8192];
			int n;
			while((n=stream.read(buffer))>=0) {
				out.write(buffer,0,n);
			}
			
			return new ProtobufBuffer(out.toByteArray());
		} catch(IOException e) {
			return BaseException.throwException(e);
		}
		throw new BaseException();
	}

	@Override
	public <T> T decode(Class<T> clazz) throws BaseException {
		TypeDefinition def=scheme.getRootType();
		ProtobufBuffer buffer=getBuffer();
		if(def!=null) {
			AbstractCodec codec=scheme.getClassCodec(context, def);
			if(codec==null) {
				Class<?> c=def.asClass(context.getTypeLoader().getClassLoader());
				PrimitiveTypeCodec primitiveTypeCodec=scheme.getPrimitiveTypeCodec(c);
				if(primitiveTypeCodec!=null) {
					return context.cast(clazz,buffer.consume(new PrimitiveTypeListener(primitiveTypeCodec)).getResult());
				} else {
					throw new BaseException(BaseException.BAD_REQUEST,"Decoding object of class "+c.getSimpleName());
				}
			} else {
				return context.cast(clazz,buffer.consume(new ClassDefListener(codec)).getResult());
			}
		}
		return null;
	}

	@Override
	public boolean hasNext() {
		return !read;
	}
	
	class PrimitiveTypeListener implements ProtobufListener {
		PrimitiveTypeCodec codec;
		Object result;
		BaseException e;
		private PrimitiveTypeListener(PrimitiveTypeCodec codec) {
			this.codec=codec;
		}
		
		private Object getResult() throws BaseException {
			if(e!=null) {
				throw e;
			} else {
				return result;
			}
		}

		@Override
		public void consume(int field, int value) {
			try {
				result=codec.decode(value);
			} catch(BaseException e) {
				this.e=e;
			}
		}

		@Override
		public void consume(int field, long value) {
			try {
				result=codec.decode(value);
			} catch(BaseException e) {
				this.e=e;
			}
		}

		@Override
		public void consume(int field, byte[] value, int offset, int len) {
			try {
				result=codec.decode(value,offset,len);
			} catch(BaseException e) {
				this.e=e;
			}
		}
	}
	
	class ClassDefListener implements ProtobufListener, AccessContext {
		ClassDefListener parent;
		ClassDefListener child;
		AbstractCodec codec;
		AccessibleObject o;
		BaseException first;
		int[] mask;
		AccessibleObject array;
		ArrayEntry arrays;

		private ClassDefListener(AbstractCodec codec) throws BaseException {
			this.codec=codec;
			this.o=codec.newAccessible(this);
			this.mask=codec.getMask().clone();
		}

		private ClassDefListener(ClassDefListener parent,AccessibleObject o,AbstractCodec codec) throws BaseException {
			this.parent=parent;
			init(o,codec);
		}
		
		private ClassDefListener init(AccessibleObject o,AbstractCodec codec) {
			this.codec=codec;
			this.o=o;
			this.mask=codec.getMask().clone();
			first=null;
			array=null;
			arrays=null;
			return this;
		}
		

		@Override
		public <T> T castTo(Class<T> clazz) {
			T t=o.castTo(getContext(),clazz);
			if(t==null) {
				return parent==null?context.castTo(clazz):parent.castTo(clazz);
			} else {
				return t;
			}
		}

		public AccessibleObject resolveObjectReference(int ref) {
			return ProtobufDecoder.this.resolveObjectReference(ref);
		}
		public void addObjectReference(AccessibleObject o) {
			ProtobufDecoder.this.addObjectReference(o);
		}
		
		public void mark(int offset) {
			codec.mark(mask, offset);
		}
		
		private Object getResult() throws BaseException {
			if(first!=null) {
				throw first;
			}
			return finalized().getAssignable();
		}
		
		public AccessibleObject finalized() throws BaseException {
			return codec.finalize(currentObject(), arrays,mask);
		}
		
		public AccessibleObject currentObject() throws BaseException {
			if(first!=null) {
				throw first;
			}
			return o;
		}

		@Override
		public <T> T castTo(Context context, Class<T> clazz) {
			return context.castTo(clazz);
		}

		@Override
		public Constructor resolve(Context context, Type type) {
			return scheme.getFactory().resolve(context, type);
		}

		@Override
		public Access resolve(Context context, TypeDefinition type) {
			return scheme.getFactory().resolve(context, type);
		}

		@Override
		public Access resolve(Access referrer, TypeDefinition type) {
			return scheme.getFactory().resolve(referrer, type);
		}

		@Override
		public Context getContext() {
			return context;
		}

		@Override
		public void consume(int field, int value) {
			consume(field,(long)value);
		}

		@Override
		public void consume(int field, long value) {
			if(first==null) try {
				codec.consume(this,field,value);
			} catch(BaseException e) {
				first=e;
			}
		}

		@Override
		public void consume(int field, byte[] value, int offset, int len) {
			if(first==null) try {
				codec.consume(this, field, value,offset,len);
			} catch(BaseException e) {
				first=e;
			}
				
		}

		@Override
		public void onError(int offset, IOException e) {
			if(first==null) {
				first=BaseException.normalize(e);
			}
		}

		public ClassDefListener createChild(AccessibleObject o,AbstractCodec classCodec) throws BaseException {
			if(child==null) {
				return child=new ClassDefListener(this, o,classCodec);
			} else {
				return child.init(o, classCodec);
			}
		}

		public AccessibleObject getArray(int offset,Creator creator) throws BaseException {
			if(arrays!=null) {
				ArrayEntry rover=arrays;
				while(rover!=null) {
					if(rover.offset==offset) {
						return rover.val;
					}
					rover=rover.next;
				}
			}
			AccessibleObject val=creator.newAccessible(this);
			arrays=new ArrayEntry(arrays, offset, val);
			return val;
		}

		public void consume(ClassDefListener listener, int field, AccessibleObject obj) throws BaseException {
			codec.consume(listener, field, obj);
		}
	}
	
	interface Creator {
		public AccessibleObject newAccessible(AccessContext context) throws BaseException;
	}
	
	static class ArrayEntry {
		public ArrayEntry next;
		public int offset;
		public AccessibleObject val;
		public ArrayEntry(ArrayEntry next,int offset,AccessibleObject val) {
			this.next=next;
			this.offset=offset;
			this.val=val;
		}
	}
}
