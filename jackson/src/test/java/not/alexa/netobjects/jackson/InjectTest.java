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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;

public class InjectTest {

	public InjectTest() {
	}
	
	@Test
	public void inject0() {
		Context context=Context.createRootContext();
		try(Decoder decoder=XMLCodingScheme.REST_SCHEME.createDecoder(context,"<object/>".getBytes())) {
			Inject0 inject=decoder.decode(Inject0.class);
			assertEquals(context,inject.context);
		} catch(Throwable e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void inject1() {
		Context context=Context.createRootContext();
		try(Decoder decoder=XMLCodingScheme.REST_SCHEME.createDecoder(context,"<object/>".getBytes())) {
			Inject1 inject=decoder.decode(Inject1.class);
			assertEquals(context,inject.context);
			assertEquals(context,inject.context2);
			assertEquals(context,inject.context3);
		} catch(Throwable e) {
			e.printStackTrace();
			fail();
		}
	}

	public static class Inject0 {
		@JacksonInject Context context;
	}

	public static class Inject1 {
		Context context;
		Context context2;
		@JacksonInject Context context3;
		@JsonCreator
		public Inject1(@JacksonInject Context context) {
			this.context=context;
		}
		
		@JacksonInject
		public void setContext(Context context) {
			this.context2=context;
		}
	}
}
