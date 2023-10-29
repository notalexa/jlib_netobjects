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

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;

public class FailureTest {

	public FailureTest() {
	}
	
	private static List<Token> illegalModifier() {
		return Collections.singletonList(new Token.SimpleToken(Type.Scalar, null, "x"));
	}
	
	@Test
	public void illegalTokenTest1() {
		try {
			new Token.SimpleToken(Type.Scalar, null, "x").decorate(new Token.SimpleToken(Type.Scalar, null, "y"));
			fail();
		} catch(Throwable t) {
		}
	}

	@Test
	public void illegalTokenTest2() {
		try {
			new Token.SimpleToken(Type.DecoratedToken, null, "x").decorate(new Token.SimpleToken(Type.Anchor,null,"anchor")).undecorate((m,t)-> {
			});
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}

	@Test
	public void illegalModifierTest3() {
		try {
			Handler handler=new YamlOutput(new ByteArrayOutputStream(), true);
			handler.beginArray(false,illegalModifier());
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}

	@Test
	public void illegalModifierTest4() {
		try {
			Handler handler=new YamlOutput(new ByteArrayOutputStream(), true);
			handler.beginArray(false,Collections.emptyList());
			handler.beginArray(false,illegalModifier());
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}
	@Test
	public void illegalModifierTest5() {
		try {
			Handler handler=new YamlOutput(new ByteArrayOutputStream(), true);
			handler.beginArray(false,Collections.emptyList());
			handler.beginObject(false,illegalModifier());
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}
	@Test
	public void illegalModifierTest6() {
		try {
			Handler handler=new YamlOutput(new ByteArrayOutputStream(), true);
			handler.scalar(false,illegalModifier(),illegalModifier().get(0));
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}

	@Test
	public void illegalModifierTest7() {
		try {
			Handler handler=new YamlOutput(new ByteArrayOutputStream(), true);
			handler.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Script, null,"script"));
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}
	
	@Test
	public void illegalModifierTest8() {
		try {
			Handler handler=new YamlOutput(new ByteArrayOutputStream(), true);
			handler.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.CurlyClose, null,"}"));
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}

	@Test
	public void illegalModifierTest9() {
		try {
			Handler handler=new JsonOutput(true,"","",new ByteArrayOutputStream());
			handler.beginArray(false,illegalModifier());
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}

	@Test
	public void illegalModifierTest10() {
		try {
			Handler handler=new JsonOutput(true,"","",new ByteArrayOutputStream());
			handler.beginArray(false,Collections.emptyList());
			handler.beginArray(false,illegalModifier());
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}
	@Test
	public void illegalModifierTest11() {
		try {
			Handler handler=new JsonOutput(true,"","",new ByteArrayOutputStream());
			handler.beginArray(false,Collections.emptyList());
			handler.beginObject(false,illegalModifier());
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}
	@Test
	public void illegalModifierTest12() {
		try {
			Handler handler=new JsonOutput(true,"","",new ByteArrayOutputStream());
			handler.scalar(false,illegalModifier(),illegalModifier().get(0));
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}

	@Test
	public void illegalModifierTest13() {
		try {
			Handler handler=new JsonOutput(true,"","",new ByteArrayOutputStream());
			handler.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Script, null,"script"));
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}
	
	@Test
	public void illegalModifierTest14() {
		try {
			Handler handler=new JsonOutput(true,"","",new ByteArrayOutputStream());
			handler.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.CurlyClose, null,"}"));
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}

	@Test
	public void simpleJsonTest1() {
		try {
			Handler handler=new JsonOutput(false,"","",new ByteArrayOutputStream());
			handler.beginDocument();
			handler.scalar(false,Collections.emptyList(),illegalModifier().get(0));
			handler.endDocument();
			handler.beginDocument();
			fail();
		} catch(YamlException t) {
		}
	}

	@Test
	public void simpleJsonTest2() {
		try {
			Handler handler=new JsonOutput(false,"","",new ByteArrayOutputStream());
			handler.beginDocument();
			handler.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Alias, null, "anchor"));
			fail();
		} catch(YamlException t) {
		}
	}

}
