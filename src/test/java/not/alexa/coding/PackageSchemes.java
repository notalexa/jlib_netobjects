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
package not.alexa.coding;

import java.util.ArrayList;
import java.util.List;

import not.alexa.netobjects.coding.AbstractTextCodingScheme;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.json.JsonCodingScheme;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;

public class PackageSchemes {
	public static final CodingScheme[] SCHEMATA= {
			ProtobufCodingScheme.DEFAULT_SCHEME,
			XMLCodingScheme.DEFAULT_SCHEME,
			JsonCodingScheme.DEFAULT_SCHEME,
			YamlCodingScheme.DEFAULT_SCHEME,
	};
	
	public static <T> List<TestData<T>> wrap(List<T> data) {
		List<TestData<T>> result=new ArrayList<>();
		for(CodingScheme scheme:SCHEMATA) {
			data.forEach((d)->result.add(new TestData<T>(scheme,d)));
			
		}
		return result;
	}
	
	public static class TestData<T> {
		CodingScheme scheme;
		T data;
		public TestData(CodingScheme scheme,T data) {
			this.scheme=scheme;
			this.data=data;
		}
		
		public CodingScheme getScheme() {
			return scheme;
		}
		
		public T getTest() {
			return data;
		}
	}
	
	public static void printOut(CodingScheme scheme,byte[] content) {
		if(scheme instanceof AbstractTextCodingScheme) try {
			System.out.write(content);
			System.out.println();
		} catch(Throwable t) {
		}
	}
}
