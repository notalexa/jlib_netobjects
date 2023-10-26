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

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import not.alexa.netobjects.coding.yaml.Token.Type;

public class TokenTest {

	public TokenTest() {
	}
	
	@Test
	public void arrayTest1() {
		Token t=new Token.SimpleToken(Type.Scalar,null,"xxx");
		assertEquals(t.getArray().get(0),t);
	}
	
	@Test
	public void arrayTest2() {
		Token t=new Token.SimpleToken(Type.Scalar,null,"xxx");
		int c=0;
		for(Map.Entry<Token,Token> entry:t.getMapArray()) {
			assertEquals(entry.getKey().getValue(),".");
			assertEquals(entry.getValue(),t);
			c++;
		}
		assertEquals(1, c);
	}

	@Test
	public void mapTest1() {
		Token t=new Token.SimpleToken(Type.Scalar,null,"xxx");
		try {
			int c=0;
			for(Map.Entry<String,Token> entry:t.getMap().entrySet()) {
				assertEquals(entry.getKey(),".");
				assertEquals(entry.getValue(),t);
				c++;
			}
			assertEquals(1, c);
		} catch(Throwable e) {
			fail();
		}
	}
	
	@Test
	public void undecorateTest1() {
		Token t=new Token.SimpleToken(Type.Scalar,null,"xxx");
		t.undecorate((modifier,m)->{
			assertEquals(t, m);
			assertEquals(0,modifier.size());
		});
	}
	
	@Test
	public void undecorateTest2() {
		Token anchor=new Token.SimpleToken(Type.Anchor, null, "anchor");
		try {
			Token t=new Token.SimpleToken(Type.Scalar,null,"xxx");
			t.decorate(Collections.emptyList()).decorate(Collections.singletonList(anchor)).undecorate((modifier,m)->{
				assertEquals(t, m);
				assertEquals(1,modifier.size());
			});
		} catch(Throwable t) {
			t.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void decorateFailureTest1() {
		try {
			Token anchor=new Token.SimpleToken(Type.Anchor, null, "anchor");
			anchor.decorate(anchor);
			fail();
		} catch(Throwable t) {
		}
	}

	@Test
	public void decorateFailureTest2() {
		try {
			Token anchor=new Token.SimpleToken(Type.Anchor, null, "anchor");
			Token t=new Token.SimpleToken(Type.Scalar,null,"xxx");
			anchor.decorate(t);
			fail();
		} catch(Throwable t) {
		}
	}
	
	@Test
	public void mapTokenTest2() {
		Token t=new MapToken();
		try {
			assertEquals(0,t.getMapArray().size());
			assertEquals(0,t.getMap().size());
		} catch(Throwable e) {
			fail();
		}
	}

	@Test
	public void mapTokenTest3() {
		MapToken t=new MapToken();
		try {
			t.add(t, t);
			assertEquals(1,t.getMapArray().size());
			assertEquals(0,t.getMap().size());
			fail();
		} catch(Throwable e) {
		}
	}


}
