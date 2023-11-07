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

import not.alexa.coding.PackageSchemes.TestData;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;

@RunWith(org.junit.runners.Parameterized.class)
public class PrimitiveTypeTest {

	@Parameters
	public static List<TestData<Class<?>>> primitiveClasses() {
		return PackageSchemes.wrap(Arrays.asList(new Class[] {
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
		}));
	}
	
	@Parameter
	public TestData<Class<?>> clazz;
	
	public PrimitiveTypeTest() {
	}
	
	@Test
	public void checkExistence() {
		assertNotNull("Primitive type "+clazz+" not defined.", PrimitiveTypeDefinition.getTypeDescription(clazz.getTest()));
	}

	@Test
	public void checkEncoding() {
		CodingScheme scheme=clazz.getScheme();//.DEFAULT_SCHEME;
        Context context=Context.createRootContext(new DefaultTypeLoader());
		try(ByteArrayOutputStream out=new ByteArrayOutputStream();
			Encoder encoder=scheme.createEncoder(context, out)) {
			TypeDefinition def=PrimitiveTypeDefinition.getTypeDescription(clazz.getTest());
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
