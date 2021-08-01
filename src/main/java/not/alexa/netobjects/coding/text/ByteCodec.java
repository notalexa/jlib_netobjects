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
import not.alexa.netobjects.coding.Encoder.Buffer;

/**
 * Codec for the primitive type {@link Byte}. Use {@link ByteCodec#INSTANCE}.
 * 
 * @author notalexa
 *
 */
public class ByteCodec implements Codec {
    public static final ByteCodec INSTANCE=new ByteCodec(16);
    private int radix;
    public ByteCodec(int radix) {
        this.radix=radix;
    }

    @Override
    public void encode(Buffer buffer, Object t) throws BaseException {
        byte b=((Byte)t);
        buffer.write(Integer.toString(b&0xff,radix));
    }

    @Override
    public Byte decode(not.alexa.netobjects.coding.Decoder.Buffer buffer) throws BaseException {
        try {
            return (byte)Integer.parseInt(buffer.getCharContent().toString(),radix);
        } catch(Throwable t) {
            return BaseException.throwException(t);
        }
   }
    
    @Override
    public String toString() {
        return "ByteCodec["+radix+"]";
    }
}
