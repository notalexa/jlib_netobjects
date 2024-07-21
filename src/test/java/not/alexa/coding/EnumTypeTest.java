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
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import not.alexa.coding.PackageSchemes.TestData;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.EnumTypeDefinition;
import not.alexa.netobjects.types.InterfaceTypeDefinition;
import not.alexa.netobjects.types.MethodTypeDefinition;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.UnknownTypeDefinition;

@RunWith(org.junit.runners.Parameterized.class)
public class EnumTypeTest {

	@Parameters
	public static List<TestData<TypeDefinition>> enumTypes() {
		return PackageSchemes.wrap(Arrays.asList(new TypeDefinition[] {
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
				ClassTypeDefinition.getTypeDescription(),
				Data.getTypeDescription()
		}));
	}
	
	@Parameter
	public TestData<TypeDefinition> typeDef;
	
	public EnumTypeTest() {
	}
	
	@Test
	public void checkEncoding() {
		CodingScheme scheme=typeDef.getScheme();//.newBuilder().setIndent("  ", "\n").build();
		DefaultTypeLoader resolver=new DefaultTypeLoader();
		Context context=Context.createRootContext(resolver);
		try(ByteArrayOutputStream out=new ByteArrayOutputStream();
			Encoder encoder=scheme.createEncoder(context, out)) {
			encoder.encode(typeDef.getTest()).flush();
			PackageSchemes.printOut(scheme,out.toByteArray());
			try(Decoder decoder=scheme.createDecoder(context, out.toByteArray())) {
				Object decoded=decoder.decode(Object.class);
				assertEquals(typeDef.getTest(),decoded);
			}
		} catch(AssertionError e) {
		    throw e;
		} catch(Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}
}
