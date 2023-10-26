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
 */
package not.alexa.netobjects.coding.text;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import not.alexa.netobjects.coding.text.LineReader.Line;

@RunWith(org.junit.runners.Parameterized.class)
public class LineReaderLineTest {
	
	private static byte[] longLine(int l) {
		byte[] val=new byte[l];
		Arrays.fill(val,(byte)'a');
		val[l-2]='\n';
		return val;
	}
	private static String longString(int l) {
		char[] val=new char[l];
		Arrays.fill(val,'a');
		return new String(val);
	}

    @Parameters
    public static List<TestData> testObjects() {
    	return Arrays.asList(
    			new TestData[] {
    					new TestData(new byte[0],new Line[0]),
    					new TestData(new byte[] {(byte)'a'},new Line[] {new Line("a",'\n')}),
    					new TestData(new byte[] {(byte)'a',(byte)'\r'},new Line[] {new Line("a",'\n')}),
    					new TestData(new byte[] {(byte)'a',(byte)'\n'},new Line[] {new Line("a",'\n')}),
    					new TestData(new byte[] {(byte)'a',(byte)'\r',(byte)'\n'},new Line[] {new Line("a",'\n')}),
    					new TestData(new byte[] {(byte)'a',(byte)'\n',(byte)'\r'},new Line[] {new Line("a",'\n'),new Line("",'\n')}),
    					new TestData(new byte[] {(byte)'a',(byte)0xe2,(byte)0x80,(byte)0xa8},new Line[] {new Line("a",'\u2028')}),
    					new TestData(new byte[] {(byte)'a',(byte)0xe2,(byte)0x80,(byte)0xa9},new Line[] {new Line("a",'\u2029')}),
    					new TestData(new byte[] {(byte)'a',},new Line[] {new Line("a",'\n')}),
    					new TestData(new byte[] {(byte)0xef,(byte)0xbb,(byte)0xbf,(byte)'a'},new Line[] {new Line("a",'\n')}),
    					new TestData(new byte[] {(byte)0xfe,(byte)0xff,0,(byte)'a'},new Line[] {new Line("a",'\n')}),
    					new TestData(new byte[] {(byte)0xff,(byte)0xfe,(byte)'a',0},new Line[] {new Line("a",'\n')}),
    					new TestData(new byte[] {0,0,(byte)0xfe,(byte)0xff,0,0,0,(byte)'a'},new Line[] {new Line("a",'\n')}),
    					new TestData(new byte[] {(byte)0xff,(byte)0xfe,0,0,(byte)'a',0,0,0},new Line[] {new Line("a",'\n')}),
    					new TestData(longLine(5000),new Line[] {new Line(longString(5000-2),'\n'),new Line("a",'\n')}),
    			}
    		);
    }

    @Parameter
    public TestData data;
    
    @Test
    public void bomTest1() {
    	try(LineReader in=new LineReader(new ByteArrayInputStream(data.data))) {
    		Line line=new Line();
    		int i=0;
    		while(in.readLine(line)) {
    			// Reader will be reset after this line
    			assertEquals(i,line.lineNo);
    			//assertEquals(0,line.charPos);
    			assertEquals(0,line.linePos);
    			assertEquals(data.result[i].length,line.length);
    			assertEquals(data.result[i].lineBreak,line.lineBreak);
    			assertEquals(data.result[i].getLine(),line.getLine());
    			assertArrayEquals(data.result[i].content,Arrays.copyOf(line.content,line.length));
    			i++;
    		}
    		in.resetCounters();
    		assertEquals(i,data.result.length);    		
    	} catch(Throwable t) {
    		t.printStackTrace();
    		fail();
    	}
    }
	
	public static class TestData {
		byte[] data;
		Line[] result;
		public TestData(byte[] data,Line[] result) {
			this.result=result;
			this.data=data;
		}
		
	}
}
