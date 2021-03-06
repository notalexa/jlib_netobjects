/*
 * Copyright (C) 2021 Not Alexa
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

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.types.ObjectType;

/**
 * Codec for the primitive type {@link ObjectType}. Use {@link ObjectTypeCodec#INSTANCE}.
 * 
 * @author notalexa
 *
 */
public class ObjectTypeCodec implements Codec {
	public static final ObjectTypeCodec INSTANCE=new ObjectTypeCodec();
	
	public ObjectTypeCodec() {
	}

	@Override
	public void encode(Encoder.Buffer buffer, Object o) throws BaseException {
		buffer.write(((ObjectType)o).getUrn());
	}

	@Override
	public ObjectType decode(Decoder.Buffer buffer) throws BaseException {
		return ObjectType.resolve(buffer.getCharContent().toString());
	}	
}
