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
package not.alexa.netobjects.jackson;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.deferred.DeferredInterfaceTest.Content;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.TypeDefinition;

public class ClassLoaderTest {

	public ClassLoaderTest() {
	}

	public void run(List<Object> ref,ReferenceQueue<Class<?>> queue) {
        try {
        	ref.add(new PhantomReference<>(TestClass.class, queue));
            TypeDefinition type=new DefaultTypeLoader(getClass().getClassLoader()).createContext().resolveType(TestClass.class);
        } catch(Throwable t) {
        	t.printStackTrace();
        }

	}

	public static class TestClass implements Content {
		@JsonProperty String s1;
		@JsonProperty String s2;
		@JsonProperty String s3;
		@JsonProperty TestClass testClass;
		@JsonProperty List<TestClass> list;
		@JsonProperty Object data;
		
		@JsonCreator
		public TestClass(@JsonProperty("s1") String s1,@JsonProperty("s2") String s2) {
			this.s1=s1;
			this.s2=s2;
			this.s3=s2;
		}
		
		@Override
		public String displayContent(Context context) {
			return "Display content of TestClass: "+s1;
		}
	}
}
