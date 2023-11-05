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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Document;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.coding.yaml.Yaml.OutputHandler;

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
			new TestToken(Type.DecoratedToken).decorate(new Token.SimpleToken(Type.Anchor,null,"anchor")).undecorate((m,t)-> {
			});
			fail();
		} catch(RuntimeException|YamlException t) {
		}
	}

	@Test
	public void illegalModifierTest3() {
		try(OutputHandler handler=new YamlOutput(new ByteArrayOutputStream(), true)) {
			handler.beginArray(false,illegalModifier());
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}

	@Test
	public void illegalModifierTest4() {
		try(OutputHandler handler=new YamlOutput(new ByteArrayOutputStream(), true)) {
			handler.beginArray(false,Collections.emptyList());
			handler.beginArray(false,illegalModifier());
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}
	@Test
	public void illegalModifierTest5() {
		try(OutputHandler handler=new YamlOutput(new ByteArrayOutputStream(), true)) {
			handler.beginArray(false,Collections.emptyList());
			handler.beginObject(false,illegalModifier());
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}
	@Test
	public void illegalModifierTest6() {
		try(OutputHandler handler=new YamlOutput(new ByteArrayOutputStream(), true)) {
			handler.scalar(false,illegalModifier(),illegalModifier().get(0));
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}

	@Test
	public void illegalModifierTest7() {
		try(OutputHandler handler=new YamlOutput(new ByteArrayOutputStream(), true)) {
			handler.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Script, null,"script"));
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}
	
	@Test
	public void illegalModifierTest8() {
		try(OutputHandler handler=new YamlOutput(new ByteArrayOutputStream(), true)) {
			handler.scalar(false,Collections.emptyList(),new TestToken(Type.CurlyClose));
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}

	@Test
	public void illegalModifierTest9() {
		try(OutputHandler handler=new JsonOutput(true,"","",new ByteArrayOutputStream())) {
			handler.beginArray(false,illegalModifier());
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}

	@Test
	public void illegalModifierTest10() {
		try(OutputHandler handler=new JsonOutput(true,"","",new ByteArrayOutputStream())) {
			handler.beginArray(false,Collections.emptyList());
			handler.beginArray(false,illegalModifier());
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}
	@Test
	public void illegalModifierTest11() {
		try(OutputHandler handler=new JsonOutput(true,"","",new ByteArrayOutputStream())) {
			handler.beginArray(false,Collections.emptyList());
			handler.beginObject(false,illegalModifier());
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}
	@Test
	public void illegalModifierTest12() {
		try(OutputHandler handler=new JsonOutput(true,"","",new ByteArrayOutputStream())) {
			handler.scalar(false,illegalModifier(),illegalModifier().get(0));
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}

	@Test
	public void illegalModifierTest13() {
		try(OutputHandler handler=new JsonOutput(true,"","",new ByteArrayOutputStream())) {
			handler.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Script, null,"script"));
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}
	
	@Test
	public void illegalModifierTest14() {
		try(OutputHandler handler=new JsonOutput(true,"","",new ByteArrayOutputStream())) {
			handler.scalar(false,Collections.emptyList(),new TestToken(Type.CurlyClose));
			fail();
		} catch(RuntimeException|IOException t) {
		}
	}

	@Test
	public void simpleJsonTest1() {
		try(OutputHandler handler=new JsonOutput(false,"","",new ByteArrayOutputStream())) {
			handler.beginDocument();
			handler.scalar(false,Collections.emptyList(),illegalModifier().get(0));
			handler.endDocument();
			handler.beginDocument();
			fail();
		} catch(IOException t) {
		}
	}

	@Test
	public void simpleJsonTest2() {
		try(OutputHandler handler=new JsonOutput(false,"","",new ByteArrayOutputStream())) {
			handler.beginDocument();
			handler.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Alias, null, "anchor"));
			fail();
		} catch(IOException t) {
		}
	}
	
	@Test
	public void failureTest2() {
		String[] result=new String[] {
			"l.3: Misplaced node (two strings?)",
			"l.6: Misplaced node (two strings?)",
			"l.9: Misplaced node (two strings?)",
			"l.12: Misplaced node (two strings?)",
			"l.16: Incomplete line",
			"l.18: Misplaced node (two strings?)",
			"l.21: Misplaced node (two strings?)",
			"l.24: Misplaced node (two strings?)",
			"l.27: Bad indentation: An indentation of length 2 was expected.",
			"l.29: Two hex digits expected after \\x",
			"l.31: Four hex digits expected after \\u",
			"l.33: Eight hex digits expected after \\U",
			"l.35: Illegal character after \\: A",
			"l.38: Missing - to indicate new array item.",
			"l.41: Unsupported script: script",
			"l.44: Bad indentation: An indentation of length 2 was expected.",
			"l.46: Misplaced :",
			"l.48: Misplaced :",
			"l.50: Misplaced :",
			"l.53: Misplaced :",
			"l.56: Misplaced key indicator",
			"l.60: Misplaced key indicator",
			"l.62: Misplaced node (two strings?)",
		};
		try(InputStream in=new ByteArrayInputStream(
				(
				 "---\na\nb\n"+
		         "---\na\n- b\n"+
		         "---\n- a\nb\n"+
		         "---\na\nc: d\n"+
		         "---\na: d\nb\n"+   
		         "---\n- a\nb: d\n"+
		         "---\nb: d\n- a\n"+   
		         "---\n  a: b\na: b\n"+
		         "---\n  a: b\n a: b\n"+
		         "---\n\"\\x2\"\n"+
		         "---\n\"\\u2\"\n"+
		         "---\n\"\\U2\"\n"+
		         "---\n\"\\A\"\n"+
				 "---\n- a\n  b\n"+
				 "---\n- @script a\n"+
				 "---\n- @identity\n  a: b\n c:d\n"+
				 "---\n: c\n"+
				 "---\n- : c\n"+
				 "---\na: c: d\n"+
				 "---\na: c\n: d\n"+
				 "---\n? c\n? d\n"+
				 "---\n?\n- a\n? b\n"+
				 "---\n\"a\" 'b'\n"+
""						).getBytes())) {
			List<Throwable> failures=new ArrayList<>();
			List<Throwable> messageFailures=new ArrayList<>();
			int c=0;
			for(Document doc:new Yaml(Mode.Indented,YamlScript.IDENTITY).parse(in)) {
				c++;
				doc.process(new Yaml.Handler() {
					@Override
					public void onError(YamlException e) throws YamlException {
						if(!result[failures.size()].equals(e.getMessage())) {
							e.printStackTrace();
							messageFailures.add(e);
						}
						failures.add(e);
					}
				});
			}
			assertEquals(result.length,failures.size());
			assertEquals(result.length, c);
			assertEquals(0, messageFailures.size());
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void simpleJsonFailureTes() {
		String[] result=new String[] {
			"l.1: Forbidden in Json: Modifier",
			"l.1: Forbidden in Json: Modifier",
			"l.1: Forbidden in Json: Modifier",
			"l.1: Forbidden in Json: Aliases",
			"l.1: Forbidden in Json: Arrays as keys",
			"l.1: Forbidden in Json: Object as keys",
			"l.1: Missing : in flow mode",
			"l.1: Value expected",
			"l.1: Misplaced separator",
			"l.1: Misplaced separator",
			"l.1: Misplaced separator",
			"l.1: Misplaced ,",
			"l.1: Misplaced ,",
			"l.1: Misplaced :",
			"l.1: Misplaced :",
			"l.1: Misplaced :",
			"l.1: Missing , or :",
		};
		List<Throwable> failures=new ArrayList<>();
		List<Throwable> messageFailures=new ArrayList<>();
		int c=0;
		for(String f:new String[] {
			"&anchor a",
			"&anchor []",
			"&anchor {}",
			"*a",
			"{[]:a}",
			"{{}:a}",
			"{a}",
			"{a:}",
			"{,a:b}",
			"[,a]",
			"{a,b}",
			"{a:b,}",
			"[a,]",
			"[a:]",
			"{:c}",
			"{a:c :}",
			"{\"a\" \"b\"}",
		}) try(InputStream in=new ByteArrayInputStream(f.getBytes())) {
		//try(InputStream in=new ByteArrayInputStream("-\n -123\n- -\n --ab".getBytes())) {
			for(Document doc:new Yaml(Mode.Json).parse(in)) {
				c++;
				doc.process(new Yaml.Handler() {
					@Override
					public void onError(YamlException e) throws YamlException {
						if(!result[failures.size()].equals(e.getMessage())) {
							e.printStackTrace();
							messageFailures.add(e);
						}
						failures.add(e);
					}
				});
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		assertEquals(result.length,failures.size());
		assertEquals(result.length, c);
		assertEquals(0, messageFailures.size());
	}
	
	private static class TestToken implements Token {
		Type type;
		private TestToken(Type type) {
			this.type=type;
		}
		@Override
		public Type getType() {
			return type;
		}
	}
	
	@Test
	public void illegalDocumentTest1() {
		try {
			new Token.SimpleToken(Type.Anchor, null, "alias").asDocument();
			fail();
		} catch(YamlException e) {
		}
		try {
			new Yaml(Mode.Indented).asDocument(new Token.SimpleToken(Type.Anchor, null, "alias"));
			fail();
		} catch(YamlException e) {
		}
	}
	
	@Test
	public void defaultHandlerFailureTest1() {
		Handler h=new Yaml.DefaultHandler(true, (t,e)-> {
		});
		try {
			h.onError(new YamlException("test"));
			fail();
		} catch(Throwable t) {
		}
		try {
			h.beginArray(true, Collections.emptyList());
			h.endArray(false);
			fail();
		} catch(YamlException e) {
		}
		try {
			h.beginObject(true, Collections.emptyList());
			h.endObject(false);
			fail();
		} catch(YamlException e) {
		}
	}

}
