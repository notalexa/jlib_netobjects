package not.alexa.coding;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.TypeLoader;

@RunWith(org.junit.runners.Parameterized.class)
public class PrimitiveTypeFailureTest {

    @Parameters
    public static List<String> testObjects() {
        return Arrays.asList(new String[] {
                "<object class=\"char\"/>",
                "<object class=\"int\"/>",
                "<object class=\"byte[]\">X</object>",
                "<object class=\"java.util.Date\"/>",
                "<object class=\"boolean\"/>"
        });
    }
    
    @Parameter
    public String s;
    
    @Test
    public void checkEncoding() {
        XMLCodingScheme scheme=XMLCodingScheme.DEFAULT_SCHEME;
        Context context=Context.createRootContext(new DefaultTypeLoader());
        try(Decoder decoder=scheme.createDecoder(context, s.getBytes())) {
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
