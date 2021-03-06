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

/**
 * Codec for the primitive type {@link String}. Use {@link StringCodec#INSTANCE}.
 * 
 * @author notalexa
 *
 */
public class StringCodec implements Codec {
	public static final StringCodec INSTANCE=new StringCodec();
	
	public StringCodec() {
	}

	@Override
	public void encode(Encoder.Buffer buffer, Object o) throws BaseException {
		buffer.write(o.toString());		
	}

	@Override
	public String decode(Decoder.Buffer buffer) throws BaseException {
		return buffer.getCharContent().toString();
	}

}
