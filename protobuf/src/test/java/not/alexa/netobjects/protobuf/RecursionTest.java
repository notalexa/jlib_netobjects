/*
 * Copyright (C) 2024 Not Alexa
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
package not.alexa.netobjects.protobuf;

import java.lang.reflect.Field;

import org.junit.Test;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.TypeDefinition;

public class RecursionTest {

	public RecursionTest() {
	}
	
	@Test public void rec() {
		try {
			Class<?> clazz=Class.forName("com.google.protobuf.DescriptorProtos$FeatureSet");
			Class<?> rover=clazz;
			for(Field f:clazz.getDeclaredFields()) {
				System.out.println(f);
			}
			while(rover!=null) {
				System.out.println(rover);
				rover=rover.getSuperclass();
			}
			TypeDefinition def=Context.createRootContext().resolveType(clazz);
			System.out.println(def);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

}
