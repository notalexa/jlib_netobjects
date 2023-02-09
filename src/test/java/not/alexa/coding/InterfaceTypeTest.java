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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import not.alexa.netobjects.types.InterfaceTypeDefinition;
import not.alexa.netobjects.types.MethodTypeDefinition;

public class InterfaceTypeTest {

	public InterfaceTypeTest() {
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
