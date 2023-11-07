/*
 * Copyright (C) 2023 Not Alexa
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import not.alexa.coding.PackageSchemes;
import not.alexa.coding.PackageSchemes.TestData;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;

@RunWith(org.junit.runners.Parameterized.class)
public class PrimitiveTypeCodingTest {

    @Parameters
    public static List<TestData<Object>> testObjects() {
        return PackageSchemes.wrap(Arrays.asList(new Object[] {
                ObjectType.createClassType(String.class),
                ObjectType.createClassType(byte[].class),
                ObjectType.createClassType(long[].class),
                ObjectType.createClassType(Object[].class),
                UUID.randomUUID(),
                new BigInteger("12345678901234567890"),
                new BigDecimal("1234567890.123456789"),
                "Hello World",
                "\nHello World\n",
                "\t",
                new Date(1000*(System.currentTimeMillis()/1000)),
                false,
                '&',
                '\'',
                (byte)127,
                (short)1024,
                1024,
                1024L,
                1.2f,
                1.2d,
                new byte[] { 1,2,3,4,5 }
        }));
    }
    
    @Parameter
    public TestData<Object> data;
    
    @Test
    public void checkEncoding() {
        CodingScheme scheme=data.getScheme();
        Context context=Context.createRootContext(new DefaultTypeLoader());
        try(ByteArrayOutputStream out=new ByteArrayOutputStream();
            Encoder encoder=scheme.createEncoder(context, out)) {
            encoder.encode(data.getTest()).flush();
            System.out.write(out.toByteArray());System.out.println();
            byte[] encoded1=out.toByteArray();
            try(Decoder decoder=scheme.createDecoder(context, encoded1)) {
                Object decoded=decoder.decode(Object.class);
                if(byte[].class.equals(data.getTest().getClass())) {
                    assertArrayEquals("Original and decoded object differ",(byte[])data.getTest(),(byte[])decoded);
                } else {
                    assertEquals("Original and decoded object differ",data.getTest(),decoded);
                }
            }
        } catch(Throwable t) {
            t.printStackTrace();
            fail(t.getMessage());
        }
    }
}
