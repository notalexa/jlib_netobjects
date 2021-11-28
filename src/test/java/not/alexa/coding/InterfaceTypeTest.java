package not.alexa.coding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import not.alexa.netobjects.types.InterfaceTypeDefinition;
import not.alexa.netobjects.types.MethodTypeDefinition;

public class InterfaceTypeTest {

	public InterfaceTypeTest() {
		// TODO Auto-generated constructor stub
	}

	@Test
	public void test() {
		InterfaceTypeDefinition def=new InterfaceTypeDefinition();
		def.createBuilder().createMethod("method").setReturnTypes(def).build().build();
		MethodTypeDefinition def1=def.getMethods()[0];
		MethodTypeDefinition def2=new MethodTypeDefinition(null,"method").createBuilder().setReturnTypes(def).build();
		assertEquals(def1.hashCode(),def2.hashCode());
		assertEquals(def1,def2);
	}

}
