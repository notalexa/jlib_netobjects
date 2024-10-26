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

import java.util.Arrays;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.BufferWriter;
import not.alexa.netobjects.coding.protobuf.ProtobufDecoder.ClassDefListener;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Deferred;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;

/**
 * Codec handling deferred objects
 * 
 * @author notalexa
 */
class DeferredCodec extends AbstractCodec {

	public DeferredCodec(Access classAccess) {
		super(classAccess);
	}

	@Override
	public boolean isArrayCodec() {
		return false;
	}

	@Override
	public int[] getMask() {
		return new int[1];
	}
	public void encode(ProtobufEncoder encoder, ProtobufBuffer buffer, Object o) throws BaseException {
	}

	@Override
	public void encode(ProtobufEncoder encoder, ProtobufBuffer buffer, int index, Object o) throws BaseException {
		Deferred<?, ?> deferred=(Deferred<?, ?>)o;
		if(deferred.isResolved()) {
			ProtobufCodingScheme scheme=encoder.getCodingScheme();
			ObjectType classType=deferred.getObjectType(scheme.getNamespace());
			if(classType!=null) {
				o=deferred.getCodingObject(encoder);
				if(o!=null) {
					TypeDefinition type=encoder.getContext().resolveType(classType);
					switch(type.getFlavour()) {
						case PrimitiveType:
							scheme.getPrimitiveTypeCodec(o.getClass()).encode(buffer,index,o);
							break;
						case EnumType:
							scheme.getEnumCodec(o.getClass()).encode(buffer,index,o);
							break;
						case ArrayType:
						case ClassType:
							byte[] content=scheme.getProtobufContent(o);
							if(content!=null) {
								buffer.write(index, content);
							} else {
								scheme.getClassCodec(encoder.getContext(),type).encode(encoder,buffer,index,o);
							}
							break;
						default: throw new BaseException(BaseException.BAD_REQUEST,"Unsupported");
					}
				}
			} else {
				throw new BaseException(BaseException.BAD_REQUEST,"Unsupported");
			}
		} else if(o instanceof BufferWriter) {
			BufferWriter object=(BufferWriter)o;
			object.write(buffer, index);
		} else {
			throw new BaseException();
		}
	}

	@Override
	public void consume(ClassDefListener listener, int field, AccessibleObject o) throws BaseException {
	}

	@Override
	public void consume(ClassDefListener listener, int field, long value) throws BaseException {
	}

	@Override
	public void consumeInternal(ClassDefListener listener, int field,Field f, long value) throws BaseException {
		DeferredProtobufObject o=new DeferredProtobufObject(listener.getCodingScheme(),listener.getContext(), value);
		listener.currentObject().setField(listener, f,access.makeAccessible(listener,o));
	}

	@Override
	public void consume(ClassDefListener listener, int field, byte[] value, int offset, int len) throws BaseException {
	}

	@Override
	public ClassDefListener createListener(ClassDefListener parent) throws BaseException {
		return parent.createChild(null,this);
	}

	@Override
	public void consume(byte[] value, int offset, int len, ClassDefListener childListener) throws BaseException {
		DeferredProtobufObject o=new DeferredProtobufObject(childListener.getCodingScheme(),childListener.getContext(), null,Arrays.copyOfRange(value, offset, offset+len));
		childListener.o=access.makeAccessible(childListener,o);
	}
}
