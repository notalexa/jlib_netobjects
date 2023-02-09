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

import not.alexa.netobjects.types.ClassTypeDefinition;

public class ErrorConditionTest {

	public ErrorConditionTest() {
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
