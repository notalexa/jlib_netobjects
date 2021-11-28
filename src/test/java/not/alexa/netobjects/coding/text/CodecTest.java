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
import not.alexa.netobjects.types.access.Constructor;

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
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public <T> T castTo(Context context, Class<T> clazz) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public Constructor resolve(Context context, Type type) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public Access resolve(Context context, TypeDefinition type) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public Access resolve(Access referrer, TypeDefinition type) {
                    // TODO Auto-generated method stub
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
