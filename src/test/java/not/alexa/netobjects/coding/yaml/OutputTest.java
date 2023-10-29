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
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.OutputHandler;

@RunWith(org.junit.runners.Parameterized.class)
public class OutputTest {

	public OutputTest() {
	}
	
    @Parameters
    public static List<TestData> testData() throws YamlException {
    	return Arrays.asList(
    			new TestData(new Token.SimpleToken(Type.Scalar, "!str", "token").decorate(modifier("")),
    					"---\n&anchor @script !!str token\n",
    					"&anchor @script \"token\""),
    			new TestData(new MapToken().decorate(modifier("")),
    					"--- &anchor @script\n{}\n",
    					"&anchor @script {}"),
    			new TestData(new SequenceToken().decorate(modifier("")),
    					"--- &anchor @script\n[]\n",
    					"&anchor @script []"),
    			new TestData(new MapToken().add(scalar("key"),
    					scalar("value").decorate(modifier(""))).decorate(modifier("root")),
    					"--- &rootanchor @rootscript\n"
    					+ "key: &anchor @script value\n",
    					"&rootanchor @rootscript {\"key\": &anchor @script \"value\"}"),
    			new TestData(new SequenceToken().add(scalar("value").decorate(modifier(""))).decorate(modifier("root")),
    					"--- &rootanchor @rootscript\n"
    					+ "- &anchor @script value\n",
    					"&rootanchor @rootscript [&anchor @script \"value\"]"),
    			new TestData(new MapToken().add(scalar("key").decorate(modifier("key")),
    					new MapToken()
    					.add(scalar("innerkey1"),scalar("value").decorate(modifier("value")))
    					.add(scalar("innerkey2"),new SequenceToken().add(scalar("v1").decorate(modifier("v1"))).add(scalar("v2").decorate(modifier("v2"))))
    					.add(new SequenceToken().add(scalar("k1").decorate(modifier("k1"))).add(scalar("k2").decorate(modifier("k2"))),new SequenceToken().add(scalar("v3").decorate(modifier("v3"))).add(scalar("v4").decorate(modifier("v4"))))
    					.add(new SequenceToken().add(scalar("k3").decorate(modifier("k3"))).add(scalar("k4").decorate(modifier("k4"))),new Token.SimpleToken(Type.Alias, null, "v3anchor"))
    					.decorate(modifier(""))).decorate(modifier("root")),
    			"--- &rootanchor @rootscript\n"
    			+ "&keyanchor @keyscript key:&anchor @script \n"
    			+ "  innerkey1: &valueanchor @valuescript value\n"
    			+ "  innerkey2: \n"
    			+ "  - &v1anchor @v1script v1\n"
    			+ "  - &v2anchor @v2script v2\n"
    			+ "  ? \n"
    			+ "  - &k1anchor @k1script k1\n"
    			+ "  - &k2anchor @k2script k2\n"
    			+ "  : \n"
    			+ "  - &v3anchor @v3script v3\n"
    			+ "  - &v4anchor @v4script v4\n"
    			+ "  ? \n"
    			+ "  - &k3anchor @k3script k3\n"
    			+ "  - &k4anchor @k4script k4\n"
    			+ "  : *v3anchor\n","&rootanchor @rootscript {&keyanchor @keyscript \"key\": &anchor @script {\"innerkey1\": &valueanchor @valuescript \"value\",\"innerkey2\": [&v1anchor @v1script \"v1\",&v2anchor @v2script \"v2\"],[&k1anchor @k1script \"k1\",&k2anchor @k2script \"k2\"]: [&v3anchor @v3script \"v3\",&v4anchor @v4script \"v4\"],[&k3anchor @k3script \"k3\",&k4anchor @k4script \"k4\"]: *v3anchor}}"),
    			new TestData(new SequenceToken().add(new MapToken().add(scalar("key").decorate(modifier("key")),
    					new MapToken()
    					.add(scalar("innerkey1"),scalar("value").decorate(modifier("value")))
    					.add(scalar("innerkey2"),new SequenceToken().add(scalar("v1").decorate(modifier("v1"))).add(scalar("v2").decorate(modifier("v2"))))
    					.add(new SequenceToken().add(scalar("k1").decorate(modifier("k1"))).add(scalar("k2").decorate(modifier("k2"))),new SequenceToken().add(scalar("v3").decorate(modifier("v3"))).add(scalar("v4").decorate(modifier("v4"))))
    					.add(new SequenceToken().add(scalar("k3").decorate(modifier("k3"))).add(scalar("k4").decorate(modifier("k4"))),new Token.SimpleToken(Type.Alias, null, "v3anchor"))
    					.decorate(modifier("")))).add(scalar("a2")).decorate(modifier("root")),
    			"--- &rootanchor @rootscript\n"
    			+ "- &keyanchor @keyscript key:&anchor @script \n"
    			+ "    innerkey1: &valueanchor @valuescript value\n"
    			+ "    innerkey2: \n"
    			+ "    - &v1anchor @v1script v1\n"
    			+ "    - &v2anchor @v2script v2\n"
    			+ "    ? \n"
    			+ "    - &k1anchor @k1script k1\n"
    			+ "    - &k2anchor @k2script k2\n"
    			+ "    : \n"
    			+ "    - &v3anchor @v3script v3\n"
    			+ "    - &v4anchor @v4script v4\n"
    			+ "    ? \n"
    			+ "    - &k3anchor @k3script k3\n"
    			+ "    - &k4anchor @k4script k4\n"
    			+ "    : *v3anchor\n"
    			+ "- a2\n","&rootanchor @rootscript [{&keyanchor @keyscript \"key\": &anchor @script {\"innerkey1\": &valueanchor @valuescript \"value\",\"innerkey2\": [&v1anchor @v1script \"v1\",&v2anchor @v2script \"v2\"],[&k1anchor @k1script \"k1\",&k2anchor @k2script \"k2\"]: [&v3anchor @v3script \"v3\",&v4anchor @v4script \"v4\"],[&k3anchor @k3script \"k3\",&k4anchor @k4script \"k4\"]: *v3anchor}},\"a2\"]")
    			);
    }
    
    @Parameter
    public TestData data;
    
	static List<Token> modifier(String prefix) {
		return Arrays.asList(new Token.SimpleToken(Type.Anchor,null,prefix+"anchor"),new Token.SimpleToken(Type.Script, null, prefix+"script"));
	}
	
	private static Token scalar(String s) {
		return new Token.SimpleToken(Type.Scalar,null, s);
	}

	@Test
	public void yamlTest() {
		if(data.yamlOut!=null) {
			ByteArrayOutputStream out=new ByteArrayOutputStream();
			try(OutputHandler testOut=new YamlOutput(out, true)) {
				testOut.write(data.token);
			} catch(IOException e) {
				fail();
			}
			System.out.println(new String(out.toByteArray()));
			assertEquals(data.yamlOut,new String(out.toByteArray()));
		}
	}
	
	@Test
	public void jsonTest() {
		if(data.jsonOut!=null) {
			ByteArrayOutputStream out=new ByteArrayOutputStream();
			try(OutputHandler testOut=new JsonOutput(true,"","",out)) {
				testOut.write(data.token);
			} catch(IOException e) {
				e.printStackTrace();
				fail();
			}
			System.out.println(new String(out.toByteArray()));
			assertEquals(data.jsonOut,new String(out.toByteArray()));
		}
	}
	
	public static class TestData {
		Token token;
		String yamlOut;
		String jsonOut;
		public TestData(Token token,String yamlOut,String jsonOut) {
			this.token=token;
			this.yamlOut=yamlOut;
			this.jsonOut=jsonOut;
		}
	}
}
