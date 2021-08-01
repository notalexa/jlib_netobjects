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
import not.alexa.netobjects.types.JavaClass;

/**
 * Default codec for enumeration types. This codec assumes, that the type <b>is resolvable by the context class loader</b> for decoding
 * and that the <code>toString()</code> method returns the correct text representation for encoding.
 * 
 * @author notalexa
 */
public class EnumCodec implements Codec {
    JavaClass.Type type;
    public EnumCodec(JavaClass.Type type) {
        this.type=type;
    }
    @Override
    public void encode(Buffer buffer, Object t) throws BaseException {
        buffer.write(t.toString());
    }
    
    /**
     * The implemenation resolves the enumeration type using the class loader of the context obtained by {@link not.alexa.netobjects.coding.Decoder.Buffer#getContext()}.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object decode(not.alexa.netobjects.coding.Decoder.Buffer buffer) throws BaseException {
        return Enum.valueOf((Class<Enum>)type.asClass(buffer.getContext().getTypeLoader().getClassLoader()),buffer.getCharContent().toString());
    }
}
