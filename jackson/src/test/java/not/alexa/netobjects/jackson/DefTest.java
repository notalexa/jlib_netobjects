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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;

public class DefTest {

	public DefTest() {
	}
	
	@Test public void test() {
		Context context=Context.createRootContext();
		TypeDefinition def=context.resolveType(AccessPoints.class);
		assertEquals(3,((ClassTypeDefinition)def).getFields().length);
	}

	public static class AccessPoints {
		@JsonProperty List<String> accesspoint;
		@JsonProperty List<String> dealer;
		@JsonProperty List<String> spclient;
		
		protected AccessPoints() {
		}
		
		private String getRandom(List<String> urls) {
			return urls!=null&&urls.size()>0?urls.get(ThreadLocalRandom.current().nextInt(urls.size())):null;
		}
		
		public String getAccessPoint() {
			return getRandom(accesspoint);
		}

		public String getDealer() {
			return getRandom(dealer);
		}

		public String getSpClient() {
			return getRandom(spclient);
		}
	}
}
