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
package not.alexa.netobjects.protobuf;

import java.nio.ByteBuffer;

import com.google.protobuf.GeneratedMessageV3;

import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.access.Access;

/**
 * Support for native protobuf encoding in {@link ProtobufCodingScheme}.
 * 
 * @author notalexa
 */
public class NativeProtobufSupport
		implements not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.NativeProtobufSupport {

	public NativeProtobufSupport() {
	}

	public byte[] getProtobufContent(Object o) {
		return o instanceof GeneratedMessageV3?((GeneratedMessageV3)o).toByteArray():null;
	}

	public AccessibleObject getProtobufObject(Access fieldAccess, byte[] value, int offset, int len) {
		Class<?> clazz=fieldAccess.getType().asClass(fieldAccess.getAccessLoader());
		if(GeneratedMessageV3.class.isAssignableFrom(clazz)) try {
			return fieldAccess.makeAccessible(clazz.getMethod("parseFrom",ByteBuffer.class).invoke(null, ByteBuffer.wrap(value, offset,len)));
		} catch(Throwable t) {
		}
		return null;
	}
}
