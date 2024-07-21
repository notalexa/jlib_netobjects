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

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.CodecType;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.PrimitiveTypeCodec;
import not.alexa.netobjects.coding.protobuf.ProtobufDecoder.ClassDefListener;
import not.alexa.netobjects.coding.protobuf.ProtobufDecoder.Creator;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;

/**
 * Base codec for array and class codecs.
 * 
 * @author notalexa
 */
abstract class AbstractCodec implements Creator {
	private static final int[] MASK=new int[32];
	static {
		for(int i=0;i<32;i++) {
			MASK[i]=~(1<<i);
		}
	}

	protected ProtobufCodingScheme scheme;
	protected Access access;

	AbstractCodec(ProtobufCodingScheme scheme,Access classAccess) {
		this.scheme=scheme;
		this.access=classAccess;
	}
	public boolean enableObjectRefs() {
		return false;
	}

	public abstract boolean isArrayCodec();
	public abstract int[] getMask();
	public ClassTypeDefinition getType() {
		return (ClassTypeDefinition)access.getType();
	}
	
	public ClassDefListener createListener(ClassDefListener parent) throws BaseException {
		return parent.createChild(access.newAccessible(parent),this);
	}
	
	public void mark(int[] marker,int offset) {
		int index=offset>>5;
		int mask=marker[index];
		int m=mask&MASK[offset&31];
		if(m!=mask) {
			marker[index]=m;
			marker[marker.length-1]++;
		}
	}

	public AccessibleObject newAccessible(AccessContext context) throws BaseException {
		return access.newAccessible(context);
	}

	public void encode(ProtobufEncoder encoder,ProtobufBuffer buffer, int index, Object o) throws BaseException {
		int ref=-1;
		if(enableObjectRefs()) {
			ref=encoder.getObjectReference(o);
		}
		if(ref<0) {
			buffer.push(index);
			encode(encoder,buffer,o);
			buffer.pop();
		} else {
			buffer.write(index, ref);
		}
	}
	
	public abstract void encode(ProtobufEncoder encoder,ProtobufBuffer buffer, Object o) throws BaseException;
	public abstract void consume(ClassDefListener listener,int field,AccessibleObject o) throws BaseException;
	public abstract void consume(ClassDefListener listener,int field, long value) throws BaseException;
	public abstract void consumeInternal(ClassDefListener listener,int field, long value) throws BaseException;
	public abstract void consume(ClassDefListener listener, int field, byte[] value, int offset, int len) throws BaseException;

	public AccessibleObject finalize(AccessibleObject o, ProtobufDecoder.ArrayEntry arrays,int[] marker) throws BaseException {
		return o;
	}
	
	/**
	 * Class organizing {@link PrimitiveTypeCodecs} and {@link AbstractCodec} entries.
	 * Each field (and the component of an array) has one of this codecs assigned.
	 * 
	 * @author notalexa
	 */
	public class CodecHolder {
		int offset;
		PrimitiveTypeCodec primitiveTypeCodec;
		AbstractCodec classCodec;
		
		CodecHolder(int offset) {
			this.offset=offset;
		}
		
		protected void resolveArrayCodec(Access fieldAccess) {
			boolean isPackable=false;
			classCodec=isPackable?new PackedArrayCodec(scheme,access,fieldAccess):new ArrayCodec(scheme,offset,fieldAccess);
		}
		
		void resolveCodec(CodecType type,Access fieldAccess) throws BaseException {
			switch(fieldAccess.getType().getFlavour()) {
				case PrimitiveType: primitiveTypeCodec=scheme.getPrimitiveTypeCodec(type,fieldAccess.getType().asClass(access.getAccessLoader()));
					break;
				case EnumType:primitiveTypeCodec=scheme.getEnumCodec(fieldAccess.getType().asClass(access.getAccessLoader()));
					break;
				case ClassType: classCodec=scheme.getClassCodec(fieldAccess);//access, fieldAccess.getType());
					if(classCodec==null) {
						classCodec=new ClassCodec(scheme, fieldAccess);
					}
					break;
				case InterfaceType: classCodec=scheme.getAnyCodec();
					break;
				case ArrayType: resolveArrayCodec(fieldAccess);
					break;
				default: throw new BaseException(BaseException.FORBIDDEN, "Encoding object of type "+fieldAccess.getType().getJavaClassType());
			}
		}
		
		public boolean encode(ProtobufEncoder encoder,ProtobufBuffer buffer,int index,Object o) throws BaseException {
			if(primitiveTypeCodec!=null) {
				primitiveTypeCodec.encode(buffer, index, o);
				return true;
			} else if(classCodec!=null) {
				classCodec.encode(encoder,buffer, index,o);
				return true;
			} else {
				return false;
			}
		}
	}
}