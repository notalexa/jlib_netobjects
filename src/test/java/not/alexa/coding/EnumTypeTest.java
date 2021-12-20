package not.alexa.coding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import not.alexa.coding.Data.State;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.ByteEncoder;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.EnumTypeDefinition;
import not.alexa.netobjects.types.InterfaceTypeDefinition;
import not.alexa.netobjects.types.MethodTypeDefinition;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader;
import not.alexa.netobjects.types.UnknownTypeDefinition;

@RunWith(org.junit.runners.Parameterized.class)
public class EnumTypeTest {

	@Parameters
	public static List<TypeDefinition> enumTypes() {
		return Arrays.asList(new TypeDefinition[] {
				new EnumTypeDefinition(Data.State.class),
				new ArrayTypeDefinition(new EnumTypeDefinition(Data.State.class)),
				new ArrayTypeDefinition(new ArrayTypeDefinition(new EnumTypeDefinition(Data.State.class))),
				new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class)),
				new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(Integer.class)),
				new MethodTypeDefinition(null,"method")
					.createBuilder()
						.setParameterTypes(PrimitiveTypeDefinition.getTypeDescription(String.class),
								new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class)))
						.setReturnTypes(PrimitiveTypeDefinition.getTypeDescription(String.class)).build(),
				new InterfaceTypeDefinition().createBuilder()
					.addType(ObjectType.createClassType(String.class))
					.createMethod("method").build().build(),
				UnknownTypeDefinition.getTypeDescription(),
				new UnknownTypeDefinition(ObjectType.resolve("jvm:not.alexa.unknown.UnknownType")),
				ClassTypeDefinition.getTypeDescription()
		});
	}
	
	@Parameter
	public TypeDefinition typeDef;
	
	public EnumTypeTest() {
	}
	
	@Test
	public void checkEncoding() {
		XMLCodingScheme scheme=XMLCodingScheme.DEFAULT_SCHEME;//.newBuilder().setIndent("  ", "\n").build();
		DefaultTypeLoader resolver=new DefaultTypeLoader();
		Context context=Context.createRootContext(resolver);
		try(ByteArrayOutputStream out=new ByteArrayOutputStream();
			Encoder encoder=scheme.createEncoder(context, out)) {
			encoder.encode(typeDef).flush();
			System.out.write(out.toByteArray());System.out.println();
			try(Decoder decoder=scheme.createDecoder(context, out.toByteArray())) {
				Object decoded=decoder.decode(Object.class);
				assertEquals(typeDef,decoded);
				try(ByteEncoder e=scheme.createEncoder(context)) {
				    assertSame(scheme, e.getCodingScheme());
					System.out.write(e.encode(decoded).asBytes()); System.out.println();
				}
                try(ByteEncoder e=scheme.createEncoder(context)) {
                    assertSame(scheme, e.getCodingScheme());
                    System.out.println(e.encode(decoded));
                }
			}
		} catch(AssertionError e) {
		    throw e;
		} catch(Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
}
