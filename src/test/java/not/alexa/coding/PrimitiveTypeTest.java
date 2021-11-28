package not.alexa.coding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader;

@RunWith(org.junit.runners.Parameterized.class)
public class PrimitiveTypeTest {

	@Parameters
	public static List<Class<?>> primitiveClasses() {
		return Arrays.asList(new Class[] {
				Object.class,
				ObjectType.class,
				UUID.class,
				BigInteger.class,
				BigDecimal.class,
				String.class,
				Date.class,
				Boolean.class,		
				Character.class,
				Byte.class,
				Short.class,
				Integer.class,
				Long.class,
				Float.class,
				Double.class,
				Boolean.TYPE,		
				Character.TYPE,
				Byte.TYPE,
				Short.TYPE,
				Integer.TYPE,
				Long.TYPE,
				Float.TYPE,
				Double.TYPE,
				byte[].class/*,
				Object.class*/
		});
	}
	
	@Parameter
	public Class<?> clazz;
	
	public PrimitiveTypeTest() {
	}
	
	@Test
	public void checkExistence() {
		assertNotNull("Primitive type "+clazz+" not defined.", PrimitiveTypeDefinition.getTypeDescription(clazz));
	}

	@Test
	public void checkEncoding() {
		XMLCodingScheme scheme=XMLCodingScheme.DEFAULT_SCHEME;
        Context context=Context.createRootContext(new DefaultTypeLoader());
		try(ByteArrayOutputStream out=new ByteArrayOutputStream();
			Encoder encoder=scheme.createEncoder(context, out)) {
			TypeDefinition def=PrimitiveTypeDefinition.getTypeDescription(clazz);
			encoder.encode(def).flush();
			System.out.write(out.toByteArray());System.out.println();
			try(Decoder decoder=scheme.createDecoder(context, out.toByteArray())) {
				Object decoded=decoder.decode(Object.class);
				assertEquals(def,decoded);
			}
		} catch(Throwable t) {
			t.printStackTrace();
			assertTrue(t.getMessage(),false);
		}
	}
}
