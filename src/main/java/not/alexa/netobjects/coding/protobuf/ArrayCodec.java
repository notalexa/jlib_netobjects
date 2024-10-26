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

import java.util.Collection;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.CodecType;
import not.alexa.netobjects.coding.protobuf.ProtobufDecoder.ClassDefListener;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.ArrayTypeAccess;

/**
 * Class used for encoding an array.
 * 
 * @author notalexa
 */
class ArrayCodec extends AbstractCodec {
	private static final int[] ARRAY_MASK={ 0 };
	private int offset;
	private ComponentCodec componentCodec;

	ArrayCodec(int offset,Access fieldAccess) {
		super(fieldAccess);
		this.offset=offset;
		componentCodec=new ComponentCodec(fieldAccess);
	}

	public boolean isArrayCodec() {
		return true;
	}
	
	public int[] getMask() {
		return ARRAY_MASK;
	}

	@Override
	public ClassDefListener createListener(ClassDefListener parent) throws BaseException {
		AccessibleObject array=parent.getArray(offset,this);
		return parent.createChild(array, this);
	}

	@Override
	public void encode(ProtobufEncoder encoder, ProtobufBuffer buffer, Object o) throws BaseException {
		throw new BaseException();
	}

	@Override
	public void encode(ProtobufEncoder encoder, ProtobufBuffer buffer, int index, Object o) throws BaseException {
		Collection<?> col=ArrayTypeAccess.canonicalize(o);
		for(Object item:col) {
			if(item!=null) {
				componentCodec.encode(encoder, buffer,index, item);
			}
		}
	}
	
	protected AccessibleObject getArray(ClassDefListener listener) throws BaseException {
		return listener.getArray(offset,this);
	}

	@Override
	public void consume(ClassDefListener listener, int field, long value) throws BaseException {
		componentCodec.consume(listener,field,value);
	}
	
	@Override
	public void consumeInternal(ClassDefListener listener,int field, Field f,long value) throws BaseException {
		componentCodec.consume(listener,field,value);
	}

	@Override
	public void consume(ClassDefListener listener, int field, byte[] value, int offset, int len) throws BaseException {
		componentCodec.consume(listener, field,value, offset, len);
	}
	
	@Override
	public void consume(ClassDefListener listener, int field, AccessibleObject o) throws BaseException {
		getArray(listener).add(o);
	}

	public class ComponentCodec extends CodecHolder {
		ComponentCodec(Access access) {
			super(ArrayCodec.this.offset);
		}

		@Override
		protected void resolveArrayCodec(ProtobufCodingScheme scheme,Access fieldAccess) {
			classCodec=new ArrayCodec(offset, fieldAccess) {
				@Override
				public ClassDefListener createListener(ClassDefListener parent) throws BaseException {
					return parent.createChild(newAccessible(parent), this);
				}

				@Override
				public void encode(ProtobufEncoder encoder, ProtobufBuffer buffer, int index, Object o)
						throws BaseException {
					buffer.push(index);
					super.encode(encoder, buffer, 1, o);
					buffer.pop();
				}

				@Override
				protected AccessibleObject getArray(ClassDefListener listener) throws BaseException {
					return listener.currentObject();
				}
				
				
			};
		}
		
		public boolean encode(ProtobufEncoder encoder,ProtobufBuffer buffer,int index,Object o) throws BaseException {
			if(!super.encode(encoder, buffer, index, o)) {
				ProtobufCodingScheme scheme=encoder.getCodingScheme();
				byte[] content=scheme.getProtobufContent(o);
				if(content==null) {
					resolveCodec(scheme,CodecType.Default, access.getComponentAccess());
					encode(encoder,buffer,index,o);
				} else {
					buffer.write(index, content);
				}
			}
			return true;
		}
		
		public void consume(ClassDefListener listener, int field, long value) throws BaseException {
			AccessibleObject array=getArray(listener);
			if(primitiveTypeCodec!=null) {
				array.add(access.getComponentAccess().makeAccessible(listener,primitiveTypeCodec.decode(value)));
			} else if(classCodec!=null) {
				AccessibleObject obj=listener.resolveObjectReference((int)value);
				if(obj!=null) {
					getArray(listener).add(obj);
				}
			} else {
				resolveCodec(listener.getCodingScheme(),CodecType.Default,access.getComponentAccess());
				// Recursion of maximum one
				consume(listener,field,value);
			}
		}
		
		public void consume(ClassDefListener listener, int field, byte[] value, int offset, int len) throws BaseException {
			AccessibleObject array=getArray(listener);
			if(primitiveTypeCodec!=null) {
				array.add(access.getComponentAccess().makeAccessible(listener,primitiveTypeCodec.decode(value,offset,len)));
			} else if(classCodec!=null) {
				ClassDefListener childListener=classCodec.createListener(listener);
				classCodec.consume(value, offset,len,childListener);
				array.add(childListener.finalized());
			} else {
				ProtobufCodingScheme scheme=listener.getCodingScheme();
				AccessibleObject fieldValue=scheme.getProtobufObject(listener,access.getComponentAccess(),value,offset,len);
				if(fieldValue!=null) {
					array.add(fieldValue);
				} else {
					resolveCodec(scheme,CodecType.Default,access.getComponentAccess());
					// Recursion of maximum one
					consume(listener,field,value,offset,len);
				}

			}
		}
	}
}
