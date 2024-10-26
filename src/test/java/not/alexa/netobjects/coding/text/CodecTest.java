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

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.RuntimeInfo;

@RunWith(org.junit.runners.Parameterized.class)
public class CodecTest {

    @Parameters
    public static List<Codec> testObjects() {
        return Arrays.asList(new Codec[] {
                ShortCodec.INSTANCE,
                IntegerCodec.INSTANCE,
                LongCodec.INSTANCE,
                FloatCodec.INSTANCE,
                DoubleCodec.INSTANCE,
                BigIntegerCodec.INSTANCE,
                ByteCodec.INSTANCE,
                BigDecimalCodec.INSTANCE
        });
    }
    
    @Parameter
    public Codec codec;
    
    @Test
    public void checkToString() {
        String s=codec.toString();
    }
    
    @Test
    public void checkDecodingFailure() {
        try {
            codec.decode(new Decoder.Buffer() {

                @Override
                public Context getContext() {
                    return null;
                }

                @Override
                public <T> T castTo(Context context, Class<T> clazz) {
                    return null;
                }

                @Override
                public RuntimeInfo resolve(Context context, Type type) {
                    return null;
                }

                @Override
                public Access resolve(Context context, TypeDefinition type) {
                    return null;
                }

                @Override
                public Access resolve(Access referrer, TypeDefinition type) {
                    return null;
                }

                @Override
                public byte[] getByteContent() {
                    return "xxx".getBytes();
                }

                @Override
                public CharSequence getCharContent() {
                    return "xxx";
                }
            });
            fail();
        } catch(Throwable t) {
            
        }
    }
}
