/*
 * Copyright (C) 2024 Not Alexa
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
package not.alexa.netobjects.jackson;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.json.JsonCodingScheme;

public class PrimitiveTypeArrayTest {

	public PrimitiveTypeArrayTest() {
	}
	
	@Test public void byteTest() {
		ByteArray test=new ByteArray("test");
		Context context=Context.createRootContext();
		try {
			byte[] encoded=JsonCodingScheme.DEFAULT_SCHEME.createEncoder(context).encode(test).asBytes();
			System.out.write(encoded);System.out.println();
			test=JsonCodingScheme.DEFAULT_SCHEME.createDecoder(context, encoded).decode(ByteArray.class);
			System.out.println(test);
			assertArrayEquals("test".getBytes(),test.bytes);
		} catch(Throwable t) {
			t.printStackTrace();
			fail();
		}
	}

	@Test public void intTest() {
		IntArray test=new IntArray(0);
		Context context=Context.createRootContext();
		try {
			byte[] encoded=JsonCodingScheme.DEFAULT_SCHEME.createEncoder(context).encode(test).asBytes();
			System.out.write(encoded);System.out.println();
			test=JsonCodingScheme.DEFAULT_SCHEME.createDecoder(context, encoded).decode(IntArray.class);
			System.out.println(test);
			assertArrayEquals(new int[] { 0, 1,2,3 },test.ints);
		} catch(Throwable t) {
			t.printStackTrace();
			fail();
		}
	}

	public static class ByteArray {
		@JsonProperty byte[] bytes;
		protected ByteArray() {}
		public ByteArray(String s) {
			bytes=s.getBytes();
		}
		@Override
		public String toString() {
			return "ByteArray["+Arrays.toString(bytes);
		}
	}
	
	public static class IntArray {
		@JsonProperty int[] ints;
		protected IntArray() {}
		public IntArray(int i) {
			ints=new int[] { i, i+1, i+2,i+3};
		}
		@Override
		public String toString() {
			return "IntArray"+Arrays.toString(ints);
		}
	}

}
