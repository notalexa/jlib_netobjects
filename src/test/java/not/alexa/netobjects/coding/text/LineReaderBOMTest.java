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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(org.junit.runners.Parameterized.class)
public class LineReaderBOMTest {

    @Parameters
    public static List<TestData> testObjects() {
    	return Arrays.asList(
    			new TestData[] {
    					new TestData(new byte[0],new int[0]),
    					new TestData(new byte[] {(byte)0xef,(byte)0xbb,(byte)0xbf},new int[] {}),
    					new TestData(new byte[] {(byte)0xfe,(byte)0xff},new int[] {}),
    					new TestData(new byte[] {(byte)0xff,(byte)0xfe},new int[] {}),
    					new TestData(new byte[] {0,0,(byte)0xfe,(byte)0xff},new int[] {}),
    					new TestData(new byte[] {(byte)0xff,(byte)0xfe,0,0},new int[] {}),
    					new TestData(new byte[] {(byte)0xef,(byte)0xbb,(byte)0xff},new int[] {0xef,0xbb,0xff}),
    					new TestData(new byte[] {(byte)0xef,(byte)0xbc,(byte)0xff},new int[] {0xef,0xbc,0xff}),
    					new TestData(new byte[] {(byte)0xfe,(byte)0xfe},new int[] {0xfe,0xfe}),
    					new TestData(new byte[] {(byte)0xff,(byte)0xff},new int[] {0xff,0xff}),
    					new TestData(new byte[] {0,0,(byte)0xfe,(byte)0xfe},new int[] {0,0,0xfe,0xfe}),
    					new TestData(new byte[] {0,0,(byte)0xff,(byte)0xfe},new int[] {0,0,0xff,0xfe}),
    					new TestData(new byte[] {0,1,(byte)0xfe,(byte)0xfe},new int[] {0,1,0xfe,0xfe}),
    					new TestData(new byte[] {(byte)0xff,(byte)0xfe,0,1},new int[] {0x100}),
    					new TestData(new byte[] {(byte)0xff,(byte)0xfe,1,1},new int[] {0x101}),
    			}
    			);
    }

    @Parameter
    public TestData data;
    
    @Test
    public void bomTest1() {
    	try(Reader in=LineReader.createReader(new ByteArrayInputStream(data.data),LineReader.ISO_8859_1)) {
    		for(int c:data.result) {
    			assertEquals(c,in.read());
    		}
    		assertEquals(-1,in.read());    		
    	} catch(Throwable t) {
    		t.printStackTrace();
    		fail();
    	}
    }

    @Test
    public void bomTest2() {
    	try(Reader in=LineReader.createReader(new FilterInputStream(new ByteArrayInputStream(data.data)) {
			@Override
			public boolean markSupported() {
				return false;
			}
    	},LineReader.ISO_8859_1)) {
    		for(int c:data.result) {
    			assertEquals(c,in.read());
    		}
    		assertEquals(-1,in.read());    		
    	} catch(Throwable t) {
    		t.printStackTrace();
    		fail();
    	}
    }

    @Test
    public void bomFailureTest1() {
    	try(Reader in=LineReader.createReader(new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException();
			}
    	},LineReader.ISO_8859_1)) {
    		in.read();
    		fail();
    	} catch(Throwable t) {
    	}
    }

    
	
	
	public static class TestData {
		byte[] data;
		int[] result;
		public TestData(byte[] data,int[] result) {
			this.result=result;
			this.data=data;
		}
		
	}
}
