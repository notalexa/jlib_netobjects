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

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeLoader;

@RunWith(org.junit.runners.Parameterized.class)
public class PrimitiveTypeCodingTest {

    @Parameters
    public static List<Object> testObjects() {
        return Arrays.asList(new Object[] {
                ObjectType.createClassType(String.class),
                ObjectType.createClassType(byte[].class),
                ObjectType.createClassType(long[].class),
                ObjectType.createClassType(Object[].class),
                UUID.randomUUID(),
                new BigInteger("12345678901234567890"),
                new BigDecimal("1234567890.123456789"),
                "Hello World",
                new Date(1000*(System.currentTimeMillis()/1000)),
                false,
                '&',
                (byte)127,
                (short)1024,
                1024,
                1024L,
                1.2f,
                1.2d,
                new byte[] { 1,2,3,4,5 }
        });
    }
    
    @Parameter
    public Object o;
    
    @Test
    public void checkEncoding() {
        XMLCodingScheme scheme=XMLCodingScheme.DEFAULT_SCHEME;
        DefaultTypeLoader resolver=new DefaultTypeLoader();
        Context context=new Context.Root() {
            @Override
            public TypeLoader getTypeLoader() {
                return resolver;
            }
        };
        try(ByteArrayOutputStream out=new ByteArrayOutputStream();
            Encoder encoder=scheme.createEncoder(context, out)) {
            encoder.encode(o).flush();
            System.out.write(out.toByteArray());System.out.println();
            byte[] encoded1=out.toByteArray();
            try(Decoder decoder=scheme.createDecoder(context, encoded1)) {
                Object decoded=decoder.decode(Object.class);
                if(byte[].class.equals(o.getClass())) {
                    assertArrayEquals("Original and decoded object differ",(byte[])o,(byte[])decoded);
                } else {
                    assertEquals("Original and decoded object differ",o,decoded);
                }
            }
        } catch(Throwable t) {
            t.printStackTrace();
            fail(t.getMessage());
        }
    }
}
