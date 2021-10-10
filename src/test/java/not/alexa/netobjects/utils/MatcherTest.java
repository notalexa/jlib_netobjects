/*
 * Copyright (C) 2021 Not Alexa
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
 */package not.alexa.netobjects.utils;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;

import not.alexa.netobjects.utils.Matcher.Equals;

@RunWith(org.junit.runners.Parameterized.class)
public class MatcherTest {
    @Parameter
    public TestCase test;
    @Parameters
    public static List<TestCase> testCases() {
        return Arrays.asList(new TestCase[] {
                new TestCase(new StringMatcher(null),null,true),
                new TestCase(new StringMatcher(null),"b",false),
                new TestCase(new StringMatcher("a"),null,false),
                new TestCase(new StringMatcher("a"),"a",true),
                new TestCase(new StringMatcher("a"),"b",false),
                new TestCase(new Matcher.Not<String>(new StringMatcher("a")),"a",false),
                new TestCase(new Matcher.Not<String>(new StringMatcher("a")),"b",true),
                new TestCase(new Matcher.Or<String>(new StringMatcher("a"),new StringMatcher("b")),"a",true),
                new TestCase(new Matcher.Or<String>(new StringMatcher("a"),new StringMatcher("b")),"b",true),
                new TestCase(new Matcher.Or<String>(new StringMatcher("a"),new StringMatcher("b")),"c",false),
                new TestCase(new Matcher.And<String>(new StringMatcher("a"),new StringMatcher("b")),"a",false),
                new TestCase(new Matcher.And<String>(new StringMatcher("a"),new StringMatcher("a")),"a",true),
                new TestCase(new Matcher.And<String>(new StringMatcher("a"),new StringMatcher("a")),"b",false)
        });
    }
    public MatcherTest() {
    }
    
    @Test
    public void check() {
        assertTrue(test.result==test.matcher.matches(test.s));
    }

    public static class StringMatcher extends Equals<String> {
        public StringMatcher(String m) {
            super(m);
        }
    }
    
    public static class TestCase {
        Matcher<String> matcher;
        String s;
        boolean result;
        public TestCase(Matcher<String> matcher,String s,boolean result) {
            this.matcher=matcher;
            this.result=result;
            this.s=s;
        }
    }
}
