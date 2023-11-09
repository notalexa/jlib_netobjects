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
package not.alexa.netobjects.coding.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;
import not.alexa.netobjects.utils.Mapper;

public class ScalarMappingScriptTest {

	public ScalarMappingScriptTest() {
	}
	
	@Test
	public void test1() {
		try {
			List<String> result=new ArrayList<>();
			Handler h=new ScalarMappingScript("map",true,(s) -> {
				return "mapped "+s;
			}).create(null, new Yaml.Handler() {

				@Override
				public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
					result.add(token.getValue());
				}
				
			});
			h.scalar(true,Collections.emptyList(),new Token.SimpleToken("test"));
			h.scalar(false,Collections.emptyList(),new Token.SimpleToken("test"));
			h.scalar(true,Collections.emptyList(),new Token.SimpleToken(Type.Alias,"test"));
			h.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Alias,"test"));
			assertEquals(Arrays.asList("mapped test","mapped test","test","test"), result);
		} catch(YamlException e) {
			fail();
		}
	}

	@Test
	public void test2() {
		YamlException ex=new YamlException();
		List<String> result=new ArrayList<>();
		try {
			Handler h=new ScalarMappingScript("map",new Mapper<String,String,IOException>() {
				@Override
				public String map(String k) throws IOException {
					throw ex;
				}
				
			}).create(null, new Yaml.Handler() {

				@Override
				public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
					result.add(token.getValue());
				}
				
			});;
			h.scalar(true,Collections.emptyList(),new Token.SimpleToken(Type.Alias,"test"));
			h.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Alias,"test"));
			h.scalar(true,Collections.emptyList(),new Token.SimpleToken("test"));
			h.scalar(false,Collections.emptyList(),new Token.SimpleToken("test"));
		} catch(YamlException e) {
			assertEquals(Arrays.asList("test","test","test"), result);
			assertEquals(ex, e);
		}
	}

}
