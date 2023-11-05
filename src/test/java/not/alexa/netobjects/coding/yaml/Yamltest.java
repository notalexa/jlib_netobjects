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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.coding.yaml.Yaml.OutputHandler;

public class Yamltest {

	public Yamltest() {
	}
	
	@Test
	public void printableTest1() {
		char[] characters= {0x9,0xa,0xd,0x85, // line break
			0x1f,0x20,0x7e,0x7f,0x9f,0xa0,0xd7ff,0xdb80,0xdbff,0xe000,0xfffd,0xffff	
		};
		boolean[] result= {true,true,true,true,
			false,true,true,false,false,true,true,false,false,true,true,false
		};
		for(int i=0;i<characters.length;i++) {
			assertEquals(result[i],Yaml.printable(characters[i]));
		}
	}
	
	@Test
	public void encodingTest1() {
		String[] data= {
				null,
				"",
				"\u0080\u0002\udb80",
				": a",
				"::",
				"::a",
				":",
				"# a",
				"a#",
				"a #",
				"a\r\n",
				"'",
				" ",
				"\u0000\u0007\u0008\u0009\n\u000b\u000c\r\u001b\"\\\u0085\u00a0\u2028\u2029",
				"a{}",
		};
		String[] result= {
				"''",
				"''",
				"\"\\x80\\x02\\udb80\"",
				"': a'",
				"'::'",
				"::a",
				"':'",
				"'# a'",
				"a#",
				"'a #'",
				"\"a\\r\\n\"",
				"''''",
				"' '",
				"\"\\0\\a\\b\\t\\n\\v\\f\\r\\e\\\"\\\\\\N\\_\\L\\P\"",
				"a{}",
		};
		for(int i=0;i<data.length;i++) {
			assertEquals(result[i],(Yaml.encode(data[i])));
		}
	}
	
	@Test
	public void encodingTest2() {
		String[] data= {
				null,
				"",
				"\u0080\u0002\udb80",
				": a",
				"::",
				"::a",
				":",
				"# a",
				"a#",
				"a #",
				"a\r\n",
				"'",
				" ",
				"\u0000\u0007\u0008\u0009\n\u000b\u000c\r\u001b\"\\\u0085\u00a0\u2028\u2029",
				"a{}",
		};
		String[] result= {
				"''",
				"''",
				"\"\\x80\\x02\\udb80\"",
				"': a'",
				"'::'",
				"'::a'",
				"':'",
				"'# a'",
				"'a#'",
				"'a #'",
				"\"a\\r\\n\"",
				"''''",
				"' '",
				"\"\\0\\a\\b\\t\\n\\v\\f\\r\\e\\\"\\\\\\N\\_\\L\\P\"",
				"'a{}'",
		};
		for(int i=0;i<data.length;i++) {
			assertEquals(result[i],(Yaml.encode(data[i],1)));
		}
	}
	
	@Test
	public void encodingTest3() {
		String[] data= {
				null,
				"",
				"\u0080\u0002\udb80",
				": a",
				"::",
				"::a",
				":",
				"# a",
				"a#",
				"a #",
				"a\r\n",
				"'",
				" ",
				"\u0000\u0007\u0008\u0009\n\u000b\u000c\r\u001b\"\\\u0085\u00a0\u2028\u2029",
				"a{}",
		};
		String[] result= {
				"\"\"",
				"\"\"",
				"\"\\x80\\x02\\udb80\"",
				"\": a\"",
				"\"::\"",
				"::a",
				"\":\"",
				"\"# a\"",
				"a#",
				"\"a #\"",
				"\"a\\r\\n\"",
				"\"'\"",
				"\" \"",
				"\"\\0\\a\\b\\t\\n\\v\\f\\r\\e\\\"\\\\\\N\\_\\L\\P\"",
				"a{}",
		};
		for(int i=0;i<data.length;i++) {
			assertEquals(result[i],(Yaml.encode(data[i],2)));
		}
	}
	
	@Test
	public void encodingTest4() {
		String[] data= {
				null,
				"",
				"\u0080\u0002\udb80",
				": a",
				"::",
				"::a",
				":",
				"# a",
				"a#",
				"a #",
				"a\r\n",
				"'",
				" ",
				"\u0000\u0007\u0008\u0009\n\u000b\u000c\r\u001b\"\\\u0085\u00a0\u2028\u2029",
				"a{}",
		};
		String[] result= {
				"\"\"",
				"\"\"",
				"\"\\x80\\x02\\udb80\"",
				"\": a\"",
				"\"::\"",
				"\"::a\"",
				"\":\"",
				"\"# a\"",
				"\"a#\"",
				"\"a #\"",
				"\"a\\r\\n\"",
				"\"'\"",
				"\" \"",
				"\"\\0\\a\\b\\t\\n\\v\\f\\r\\e\\\"\\\\\\N\\_\\L\\P\"",
				"\"a{}\"",
		};
		for(int i=0;i<data.length;i++) {
			assertEquals(result[i],(Yaml.encode(data[i],3)));
		}
	}
	
	@Test
	public void encodingTest5() {
		String[] data= {
				null,
				"",
				"\u0080\u0002\udb80",
				": a",
				"::",
				"::a",
				":",
				"# a",
				"a#",
				"a #",
				"a\r\n",
				"'",
				" ",
				"\u0000\u0007\u0008\u0009\n\u000b\u000c\r\u001b\"\\\u0085\u00a0\u2028\u2029",
				"a{}",
		};
		String[] result= {
				"''",
				"''",
				"\"\\x80\\x02\\udb80\"",
				"': a'",
				"'::'",
				"::a",
				"':'",
				"'# a'",
				"a#",
				"'a #'",
				"\"a\\r\\n\"",
				"''''",
				"' '",
				"\"\\0\\a\\b\\t\\n\\v\\f\\r\\e\\\"\\\\\\N\\_\\L\\P\"",
				"\"a{}\"",
		};
		for(int i=0;i<data.length;i++) {
			assertEquals(result[i],(Yaml.encode(data[i],true,0)));
		}
	}
	
	@Test
	public void documentTest() {
		Yaml yaml=new Yaml(Mode.Json);
		try(OutputHandler out=yaml.createOutput("  ","\n", new ByteArrayOutputStream())) {
			yaml.asDocument(new Token.SimpleToken(Type.Scalar, null, "scalar")).process(out);
			out.flush();
		} catch(IOException e) {
			fail();
		}
	}
}
