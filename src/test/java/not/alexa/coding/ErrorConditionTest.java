package not.alexa.coding;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.MethodTypeDefinition;

public class ErrorConditionTest {

	public ErrorConditionTest() {
		// TODO Auto-generated constructor stub
	}
	
	@Test
	public void wrongFieldType1() {
		try {
			// Anonymous and not immutable
			ClassTypeDefinition def=new ClassTypeDefinition();
			def=def.createBuilder()
				.addField("field",def)
				.build();
		} catch(Throwable t) {
			return;
		}
		fail("Forbidden type definition");
	}

	@Test
	public void wrongFieldType2() {
		try {
			// Anonymous and not immutable
			ClassTypeDefinition def=new ClassTypeDefinition();
			def=def.createBuilder()
				.addField("field",def.fix())
				.build();
		} catch(Throwable t) {
			return;
		}
		fail("Forbidden type definition");
	}

//	@Test
//	public void wrongReturnType() {
//		try {
//			// Anonymous and not immutable
//			ClassTypeDefinition def=new ClassTypeDefinition();
//			MethodTypeDefinition method=new MethodTypeDefinition("method").createBuilder()
//				.setReturnTypes(def).build();
//		} catch(Throwable t) {
//			return;
//		}
//		fail("Forbidden type definition");
//	}
//	
//	@Test
//	public void wrongParameterType() {
//		try {
//			// Anonymous and not immutable
//			ClassTypeDefinition def=new ClassTypeDefinition();
//			MethodTypeDefinition method=new MethodTypeDefinition("method").createBuilder()
//				.setParameterTypes(def).build();
//		} catch(Throwable t) {
//			return;
//		}
//		fail("Forbidden type definition");
//	}

}
