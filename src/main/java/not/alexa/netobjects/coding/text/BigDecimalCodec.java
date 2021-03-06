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

import java.math.BigDecimal;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Encoder.Buffer;

/**
 * Codec for the primitive type {@link BigDecimal}. Use {@link BigDecimalCodec#INSTANCE}.
 * 
 * @author notalexa
 *
 */
public class BigDecimalCodec implements Codec {
    public static final BigDecimalCodec INSTANCE=new BigDecimalCodec();

    @Override
    public void encode(Buffer buffer, Object t) throws BaseException {
        buffer.write(t.toString());
    }

    @Override
    public BigDecimal decode(not.alexa.netobjects.coding.Decoder.Buffer buffer) throws BaseException {
        return new BigDecimal(buffer.getCharContent().toString());
    }
}
