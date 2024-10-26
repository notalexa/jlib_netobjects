package not.alexa.netobjects.types.access;

import java.util.Collections;

import org.junit.Test;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.ByteEncoder;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.DeferredObject;
import not.alexa.netobjects.types.EnumTypeDefinition;
import not.alexa.netobjects.types.InterfaceTypeDefinition;
import not.alexa.netobjects.types.JavaClass;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader;
import not.alexa.netobjects.types.TypeResolver;
import not.alexa.netobjects.types.TypeResolver.LoaderIntermediate;

public class DeferredAccessTest {
	static final ClassTypeDefinition TEST_TYPE=new ClassTypeDefinition(JavaClass.getJavaNamespace().create("de.notalexa.Dummy"));
	static {
			TEST_TYPE.createBuilder()
			.addField("a",PrimitiveTypeDefinition.getTypeDescription(String.class))
			.createField("b",TEST_TYPE).setOptional(true).build()
			.createField("d",new InterfaceTypeDefinition(TestInterface.class)).setOptional(true).build()
			.addField("c",new EnumTypeDefinition(JavaClass.getJavaNamespace().create("de.notalexa.Dummy"), new EnumTypeDefinition.Value[] {
					new EnumTypeDefinition.Value(0,"start"),
					new EnumTypeDefinition.Value(1,"stop")
			}))
			.build();
	}
	
	static final String VALUE="class: de.notalexa.Dummy\na: test\nb:\n  a: test\n  c: stop\nc: start\nd:\n  class: de.notalexa.Unknown\n  alpha:\n  - a\n  - b";
	
	
	public DeferredAccessTest() {
	}
	
	@Test
	public void deferredTest1() {
		TypeLoader loader=new DefaultTypeLoader(null,null,Collections.singletonList(new TypeResolver() {
			@Override
			public TypeDefinition resolve(LoaderIntermediate intermediate, ObjectType type) {
				if(type.equals(JavaClass.getJavaNamespace().create("de.notalexa.Dummy"))) {
					return TEST_TYPE;
				} else {
					return null;
				}
			}
		}));
		Context context=Context.createRootContext(loader);
		try(Decoder decoder=YamlCodingScheme.DEFAULT_SCHEME.createDecoder(context,VALUE.getBytes())) {
			TestInterface o=decoder.decode(TestInterface.class);
			if(o!=null) {
				o.helloWorld();
			}
			System.out.println(o);
			try(ByteEncoder encoder=YamlCodingScheme.DEFAULT_SCHEME.createEncoder(context)) {
				System.out.write(encoder.encode(o).asBytes());
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	public interface TestInterface {
		public default void helloWorld() {
			System.out.println("Hello world");
		}
	}
}
