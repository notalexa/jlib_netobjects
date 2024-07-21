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

import java.lang.reflect.Array;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.PrimitiveTypeCodec;

/**
 * Codec for enumeration classes.
 * @author notalexa
 */
class EnumCodec implements PrimitiveTypeCodec {
	private Object values;
	
	EnumCodec(Class<?> clazz) {
		try {
			values=clazz.getMethod("values").invoke(null);
		} catch(Throwable t) {
			throw new RuntimeException("Method 'values' not found in "+clazz.getName()+". Is this an enum class?");
		}
	}

	@Override
	public void encode(ProtobufBuffer buffer, int field, Object o) {
		buffer.write(field, ((Enum<?>)o).ordinal());
	}

	@Override
	public Object decode(long value) throws BaseException {
		try {
			return Array.get(values,(int)value);
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
	}
}
