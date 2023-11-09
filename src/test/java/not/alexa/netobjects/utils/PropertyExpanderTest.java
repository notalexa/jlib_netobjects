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
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(org.junit.runners.Parameterized.class)
public class PropertyExpanderTest {
    @Parameter
    public TestCase test;
    
    @Parameters
    public static List<TestCase> testCases() {
        return Arrays.asList(new TestCase[] {
        		new TestCase(createExpander(0),"abc","abc"),
        		new TestCase(createExpander(3),"${prop}","propvalue"),
        		new TestCase(createExpander(3),"${user.name}",System.getProperty("user.name")),
        		new TestCase(createExpander(3),"${PATH}",System.getenv("PATH")),
        		new TestCase(createExpander(3),"${PATH:}",System.getenv("PATH")),
        		new TestCase(createExpander(3),"${PATH:xyz}",System.getenv("PATH")),
        		new TestCase(createExpander(3),"${PATH:${user.name:unknown}}",System.getenv("PATH")),
        		new TestCase(createExpander(3),"${PATH:${user.name}}",System.getenv("PATH")),
        		new TestCase(createExpander(3),"${_PATH:}",""),
        		new TestCase(createExpander(3),"${_PATH:xyz}","xyz"),
        		new TestCase(createExpander(3),"${_PATH:${user.name}",System.getProperty("user.name")),
        		new TestCase(createExpander(3),"${user.home}/lib,${user.home}/bin",System.getProperty("user.home")+"/lib,"+System.getProperty("user.home")+"/bin"),
        		new TestCase(createExpander(3),"${_PATH:[1]}${PATH}}","}"+System.getenv("PATH")),
        		new TestCase(createExpander(3),"${_PATH:[1]}${PATH}}}","}"+System.getenv("PATH")+"}"),
        		new TestCase(createExpander(3),"${x${}:xxx}",null),
        		new TestCase(createExpander(3),"${x${ab:}:xxx}",null),
        		new TestCase(createExpander(3),"\\${xxx}","${xxx}"),
        		new TestCase(createExpander(3),"${_PATH:\\${}","${"),
        		new TestCase(createExpander(3),"${PATH:\\${}",System.getenv("PATH")),
        		new TestCase(createExpander(3),"${_PATH}",null),
        		new TestCase(createExpander(3),"${user.name0:[}","["),
        		new TestCase(createExpander(3),"${user.name0:[]}","[]"),
        		new TestCase(createExpander(3),"${user.name0:[a]}","[a]"),
        		new TestCase(createExpander(2),"${user.name:xxx}","xxx"),
        		new TestCase(createExpander(1),"${PATH:xxx}","xxx"),
        });
    }

	public PropertyExpanderTest() {
	}
	
	private static PropertyExpander createExpander(int flags) {
		return flags==0?new PropertyExpander():new PropertyExpander(flags,Collections.singletonMap("prop","propvalue"));
	}
	
	@Test
	public void test1() {
		try {
			PropertyExpander expander=test.expander;
			assertEquals(test.value,expander.map(test.src));
		} catch(IllegalArgumentException e) {
			assertNull(test.value);
		}
	}
	
	public static class TestCase {
		PropertyExpander expander;
		String src;
		String value;
		public TestCase(PropertyExpander expander,String src,String value) {
			this.expander=expander;
			this.src=src;
			this.value=value;
		}
	}

}
