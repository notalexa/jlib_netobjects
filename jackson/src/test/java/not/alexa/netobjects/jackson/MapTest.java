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
package not.alexa.netobjects.jackson;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.CodingHint;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.json.JsonCodingScheme;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;

@RunWith(org.junit.runners.Parameterized.class)
public class MapTest {

	public MapTest() {
	}
	

    @Parameters
    public static List<CodingScheme> testObjects() {
        return Arrays.asList(new CodingScheme[] {
        		JsonCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ","\n").build(),
        		YamlCodingScheme.DEFAULT_SCHEME
        });
    }
    
    @Parameter
    public CodingScheme scheme;

	@Test public void testData() {
		Context context=Context.createRootContext();
		TypeDefinition def=context.resolveType(Data.class);
		assertEquals(2,((ClassTypeDefinition)def).getFields().length);
	}

	@Test public void testA() {
		Context context=Context.createRootContext();
		try {
			byte[] content=scheme.createEncoder(context).encode(new A().init()).asBytes();
			System.out.write(content);System.out.println();
			try(Decoder decoder=scheme.createDecoder(context, content)) {
				System.out.println(decoder.decode(Object.class));
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Test public void testB() {
		Context context=Context.createRootContext();
		try {
			byte[] content=scheme.createEncoder(context).encode(new B().init()).asBytes();
			System.out.write(content);System.out.println();
			try(Decoder decoder=scheme.createDecoder(context, content)) {
				System.out.println(decoder.decode(Object.class));
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Test public void testC() {
		Context context=Context.createRootContext();
		try {
			byte[] content=scheme.createEncoder(context).encode(new C().init()).asBytes();
			System.out.write(content);System.out.println();
			try(Decoder decoder=scheme.createDecoder(context, content)) {
				System.out.println(decoder.decode(Object.class));
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Test public void testD() {
		Context context=Context.createRootContext();
		try {
			byte[] content=scheme.createEncoder(context).encode(new D().init()).asBytes();
			System.out.write(content);System.out.println();
			try(Decoder decoder=scheme.createDecoder(context, content)) {
				System.out.println(decoder.decode(Object.class));
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Test public void testE() {
		Context context=Context.createRootContext();
		try {
			byte[] content=scheme.createEncoder(context).encode(new E().init()).asBytes();
			System.out.write(content);System.out.println();
			try(Decoder decoder=scheme.createDecoder(context, content)) {
				System.out.println(decoder.decode(Object.class));
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	public static class Data {
		@JsonProperty String a;
		@JsonProperty String b;
		Data() {}
		Data(String a,String b) {
			this.a=a;
			this.b=b;
		}
		
		public String getA() {
			return a;
		}
		
		public String getA0() {
			return a;
		}
		
		public String toString() {
			return "["+a+","+b+"]";
		}
		
	}

	public static class A {
		@CodingHint("inline") @JsonProperty Map<String,String> map=new HashMap<>();
		
		public String toString() {
			return "A"+map;
		}
		
		public A init() {
			map.put("a","b");
			map.put("b","c");
			map.put("c","d");
			return this;			
		}
	}
	public static class B {
		@CodingHint("inline") @CodingHint("outline") @JsonProperty Map<String,Data> map=new HashMap<>();
		
		public String toString() {
			return "B"+map;
		}
		
		public B init() {
			map.put("a",new Data("a","b"));
			map.put("b",new Data("b","c"));
			map.put("c",new Data("c","d"));
			return this;
		}
	}

	public static class C {
		@CodingHint("inline") @JsonProperty Map<Data,String> map=new HashMap<>();
		
		public String toString() {
			return "C"+map;
		}
		
		public C init() {
			map.put(new Data("a","b"),"aa");
			map.put(new Data("b","c"),"bb");
			map.put(new Data("c","d"),"cc");
			return this;
		}
	}
	
	public static class D {
		@CodingHint("inline") @JsonProperty Map<Data,Data> map=new HashMap<>();
		
		public String toString() {
			return "C"+map;
		}
		
		public D init() {
			map.put(new Data("a","b"),new Data("a","b"));
			map.put(new Data("b","c"),new Data("b","c"));
			map.put(new Data("c","d"),new Data("c","d"));
			return this;
		}
	}
	
	public static class E {
		@CodingHint("inline") @JsonProperty Map<Object,String> map=new HashMap<>();
		
		public String toString() {
			return "A"+map;
		}
		
		public E init() {
			map.put("a","b");
			map.put(new Data("b","b"),"c");
			map.put("c","d");
			return this;			
		}
	}
}
