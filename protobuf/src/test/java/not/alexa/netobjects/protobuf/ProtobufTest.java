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
package not.alexa.netobjects.protobuf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.notalexa.proto.test.ProtoTestV3.ItemState;
import de.notalexa.proto.test.ProtoTestV3.TestItem;
import de.notalexa.proto.test.ProtoTestV3.TestPage;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.CodingHint;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme;

public class ProtobufTest {
	
	private Data createData(int length) {
		TestPage page=TestPage.newBuilder().setBooleanValue(true).setIntValue(11196536).setStringValue("string")
		.setFloatValue(0f)
		.setDoubleValue(0d)
		.addItems(TestItem.newBuilder().setItemName("Item1").setState(ItemState.ACTIVE).build())
		.addItems(TestItem.newBuilder().setItemName("Item2").setState(ItemState.PENDING).build())
		.build();
		Data data=new Data(2,"a",1f,new byte[length],
				new Data(-2,"b",2f,new byte[length],
						new Data(4,"c",4f,new byte[length],
								null,page),page),page);
		return data;
	}

	@Test
	public void test10() {
		test(createData(10));
	}

	@Test
	public void test8192() {
		test(createData(8192));
	}
	
	@Test
	public void testDirectType() {
		Data data=createData(10);
		Context context=Context.createRootContext();
		CodingScheme scheme=ProtobufCodingScheme.DEFAULT_SCHEME.newBuilder().setRootType(Data.class).build();
		try {
			byte[] content0=scheme.createEncoder(context).encode(data).asBytes();
			// Field 1 with encoding 0 (int)
			assertEquals(8, content0[0]);
			Data d=scheme.createDecoder(context, content0).decode(Data.class);
			byte[] content1=scheme.createEncoder(context).encode(d).asBytes();
			assertArrayEquals(content0,content1);
		} catch(Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}

	@Test
	public void testDirectIntegerType() {
		Context context=Context.createRootContext();
		CodingScheme scheme=ProtobufCodingScheme.DEFAULT_SCHEME.newBuilder().setRootType(Integer.class).build();
		try {
			byte[] content0=scheme.createEncoder(context).encode(2l).asBytes();
			// Field 1 with encoding 0 (int)
			assertEquals(8, content0[0]);
			int d=scheme.createDecoder(context, content0).decode(Integer.class);
			byte[] content1=scheme.createEncoder(context).encode(d).asBytes();
			assertArrayEquals(content0,content1);
		} catch(Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}

	private void test(Data data) {
		Context context=Context.createRootContext();
		CodingScheme scheme=ProtobufCodingScheme.DEFAULT_SCHEME;
		try {
			byte[] content0=scheme.createEncoder(context).encode(data).asBytes();
			Data d=scheme.createDecoder(context, content0).decode(Data.class);
			byte[] content1=scheme.createEncoder(context).encode(d).asBytes();
			assertArrayEquals(content0,content1);
		} catch(Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
	}

	public static class Data {
		@CodingHint("protobuf:signed") @JsonProperty int intValue;
		@CodingHint("protobuf:fixed") @JsonProperty int fixedValue;
		@CodingHint("protobuf:fixed") @JsonProperty long longValue;
		@JsonProperty float floatValue;
		@JsonProperty Date dateValue;
		@JsonProperty UUID uuidValue;
		@JsonProperty(defaultValue="a") String stringValue;
		@JsonProperty(index=101) Data data;
		@JsonProperty TestPage page;
		@JsonProperty byte[] raw;
		@JsonProperty Object o;
		@JsonProperty Type type;
		@JsonProperty Data[] complexArray;
		@JsonProperty Data[][] doubleComplexArray;
		@JsonProperty Map<String,Data> complexMap;
		@JsonProperty int[] intArray;
		Data() {}
		public Data(int intValue,String stringValue,float floatValue,byte[] raw,Data data,TestPage page) {
			this.intValue=intValue;
			this.fixedValue=intValue;
			this.longValue=intValue;
			this.intArray=new int[] { intValue };
			this.stringValue=stringValue;
			this.floatValue=floatValue;
			this.dateValue=new Date();
			this.uuidValue=UUID.randomUUID();
			this.data=data;
			this.page=page;
			this.raw=raw;
			this.o=stringValue;
			this.o=Type.Alpha;
			this.type=Type.Alpha;
			this.complexArray=data==null?null:new Data[] { data };
			this.doubleComplexArray=data==null?null:new Data[][] { new Data[] { data }};
			if(data!=null) {
				this.o=page==null?null:new TestPage[] { page };
				//complexMap=Collections.singletonMap(stringValue, data);
			}
		}
		
		public String toString() {
			return toString("");
		}
		public String toString(String indent) {
			return "\n"+indent+"Data["+intValue+", "+stringValue+", "+type+", "+floatValue+", "+dateValue+", "+uuidValue+"]"+" "+o+(data==null?"":data.toString(indent+"  "));
		}
	}

	public enum Type {
		Alpha;
	}
}
