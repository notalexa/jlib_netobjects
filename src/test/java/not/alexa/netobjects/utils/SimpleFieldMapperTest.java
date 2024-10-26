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
package not.alexa.netobjects.utils;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import not.alexa.netobjects.types.access.RuntimeInfo.FieldMapper;

public class SimpleFieldMapperTest {

	public SimpleFieldMapperTest() {
	}
	
	@Test
	public void test() {
		Map<String,String> fieldMap=Collections.singletonMap(getClass().getName()+"#field", "property");
		FieldMapper mapper=new SimpleFieldMapper(fieldMap);
		assertEquals("property", mapper.mapField(getClass(),"field"));
		assertEquals("field", mapper.mapField(getClass().getSuperclass(),"field"));
	}

}
