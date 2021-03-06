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

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.text.ISO8601DateFormat.Format;
import not.alexa.netobjects.coding.text.ISO8601DateFormat.Precision;

/**
 * Codec for the primitive type {@link Date}. The default instance {@link DateCodec#INSTANCE}.
 * uses the {@link ISO8601DateFormat} with the format {@link ISO8601DateFormat.Format#ISO2014Long},
 * precision {@link ISO8601DateFormat.Precision#Millisecond} and timezone {@link GMT}.
 * 
 * 
 * @author notalexa
 * @see ISO8601DateFormat
 */
public class DateCodec implements Codec {
    public static final DateCodec INSTANCE=new DateCodec(new ISO8601DateFormat(Format.ISO2014Long,Precision.Millisecond,TimeZone.getTimeZone("GMT")));
    
	private DateFormat format;
	public DateCodec(DateFormat format) {
		this.format=format;
	}
	@Override
	public void encode(Encoder.Buffer buffer, Object o) throws BaseException {
		synchronized(format) {
			buffer.write(format.format(o));
		}
	}
	
	@Override
	public Date decode(Decoder.Buffer buffer) throws BaseException {
		try {
			synchronized(format) {
				return format.parse(buffer.getCharContent().toString());
			}
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
	}
}
