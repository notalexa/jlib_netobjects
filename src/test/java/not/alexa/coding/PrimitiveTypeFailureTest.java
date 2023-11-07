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
package not.alexa.coding;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;

import not.alexa.coding.PackageSchemes.TestData;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.types.DefaultTypeLoader;

@RunWith(org.junit.runners.Parameterized.class)
public class PrimitiveTypeFailureTest {

    @Parameters
    public static List<TestData<String>> testObjects() {
        return PackageSchemes.wrap(Arrays.asList(new String[] {
                "<object class=\"char\"/>",
                "<object class=\"int\"/>",
                "<object class=\"byte[]\">X</object>",
                "<object class=\"java.util.Date\"/>",
                "<object class=\"boolean\"/>"
        }));
    }
    
    @Parameter
    public TestData<String> s;
    
    @Test
    public void checkEncoding() {
        CodingScheme scheme=s.getScheme();
        Context context=Context.createRootContext(new DefaultTypeLoader());
        try(Decoder decoder=scheme.createDecoder(context, s.getTest().getBytes())) {
            try {
                Object decoded=decoder.decode(Object.class);
                fail(s+": failure expectd");
            } catch(AssertionError e) {
                throw e;
            } catch(Throwable t) {
            }
        } catch(AssertionError e) {
            throw e;
        } catch(Throwable t) {
            t.printStackTrace();
            fail(t.getMessage());
        }
    }
}
