/*
 * Copyright (C) 2025 Not Alexa
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
package not.alexa.netobjects.coding.xml;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.charset.Charset;

import org.junit.Test;

import not.alexa.coding.Data;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.ByteEncoder;
import not.alexa.netobjects.coding.Decoder;

public class RestTest {
	
	private static final byte[] MSG1="<?xml version=\"1.0\" encoding=\"UTF-8\"?><object index=\"1\" state=\"active\"><text>test</text><ref obj-ref=\"0\"/><data obj-ref=\"0\"/><list is-empty=\"true\"/><matrix><matrix is-empty=\"true\"/></matrix><matrix><matrix is-empty=\"true\"/></matrix><map is-empty=\"true\"/></object>".getBytes(Charset.forName("UTF8"));
	private static final byte[] MSG2="<?xml version=\"1.0\" encoding=\"UTF-8\"?><data index=\"1\" state=\"active\"><text>test</text><ref obj-ref=\"0\"/><data obj-ref=\"0\"/><list is-empty=\"true\"/><matrix><matrix is-empty=\"true\"/></matrix><matrix><matrix is-empty=\"true\"/></matrix><map is-empty=\"true\"/></data>".getBytes(Charset.forName("UTF8"));

	public RestTest() {
	}
	
	@Test
	public void serializeTest() {
		Data data=new Data("test",1);
		Context context=Context.createRootContext();
		try(ByteEncoder encoder=XMLCodingScheme.REST_SCHEME.createEncoder(context)) {
			assertArrayEquals(MSG1,encoder.encode(data).asBytes());
		} catch(BaseException e) {
			fail();
		}
	}

	@Test
	public void deserializeTest1() {
		Context context=Context.createRootContext();
		try(Decoder decoder=XMLCodingScheme.REST_SCHEME.createDecoder(context,MSG1)) {
			Data data=decoder.decode(Data.class);
			assertNotNull(data);
		} catch(BaseException e) {
			fail();
		}		
	}
	
	@Test
	public void deserializeTest2() {
		Context context=Context.createRootContext();
		try(Decoder decoder=XMLCodingScheme.REST_SCHEME.createDecoder(context,MSG2)) {
			Data data=decoder.decode(Data.class);
			assertNotNull(data);
		} catch(BaseException e) {
			e.printStackTrace();
			fail();
		}		
	}
}
