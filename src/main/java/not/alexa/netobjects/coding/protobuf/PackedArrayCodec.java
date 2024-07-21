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
import not.alexa.netobjects.coding.protobuf.ProtobufDecoder.ClassDefListener;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.access.Access;

/**
 * Codec for arrays implementing the packed strategy. This codec is currently not used.
 * 
 * @author notalexa
 */
class PackedArrayCodec extends AbstractCodec {

	PackedArrayCodec(ProtobufCodingScheme scheme, Access classAccess,Access fieldAccess) {
		super(scheme, classAccess);
	}

	@Override
	public void encode(ProtobufEncoder encoder, ProtobufBuffer buffer, Object o) throws BaseException {
	}

	@Override
	public void consume(ClassDefListener listener, int field, long value) throws BaseException {
	}

	@Override
	public void consume(ClassDefListener listener, int field, byte[] value, int offset, int len) throws BaseException {
	}
	@Override
	public void consumeInternal(ClassDefListener listener,int field, long value) throws BaseException {
	}
	@Override
	public void consume(ClassDefListener listener, int offset,AccessibleObject o) throws BaseException {
	}

	@Override
	public boolean isArrayCodec() {
		return true;
	}

	public int[] getMask() {
		return null;
	}
}
