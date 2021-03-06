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
 * Codec for the primitive type {@link Boolean}. Use {@link BooleanCodec#INSTANCE}.
 * 
 * @author notalexa
 *
 */
public class BooleanCodec implements Codec {
	public static final BooleanCodec INSTANCE=new BooleanCodec();
	
	public BooleanCodec() {
	}

	@Override
	public void encode(Encoder.Buffer buffer, Object o) throws BaseException {
		buffer.write(o.toString());
	}

	@Override
	public Boolean decode(Decoder.Buffer buffer) throws BaseException {
		String s=buffer.getCharContent().toString();
		if("false".equals(s)) {
			return Boolean.FALSE;
		} else if("true".equals(s)) {
			return Boolean.TRUE;
		} else {
			throw new BaseException(BaseException.BAD_REQUEST,s+" is not a boolean value (true|false)");
		}
	}

}
