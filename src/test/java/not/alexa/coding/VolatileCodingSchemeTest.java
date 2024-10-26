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

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;

import org.junit.Ignore;
import org.junit.Test;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.coding.yaml.Yaml;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.access.AccessFactory;

public class VolatileCodingSchemeTest {

	public VolatileCodingSchemeTest() {
	}
	
	@Test
	public void testXML() throws Throwable {
		//XMLCodingScheme scheme=XMLCodingScheme.builder().setIndent("  ","\r\n").setRootTag("root").build();//new XMLCodingScheme();
		Context context=Context.createRootContext(new DefaultTypeLoader());
		ObjectType.createClassType(PrimitiveTypeDefinition.class);
		for(int i=0;i<10;i++) {
			CodingScheme scheme=new XMLCodingScheme(XMLCodingScheme.defaultCharset(),AccessFactory.getDefault()).newBuilder().setRootTag("test").setRootType(Object.class).build();
			for(Object o:new Object[] {new not.alexa.coding.Data("Hello World",100,"T1","T2"),
					/*"\"Hello&World\"",Data.State.active,
					1,
					ObjectType.createClassType(PrimitiveTypeDefinition.class),
					ObjectType.resolve("oid:2.3.1.24.2")*/}) try(ByteArrayOutputStream out=new ByteArrayOutputStream();
				Encoder encoder=scheme.createEncoder(context, out)) {
				encoder.encode(o).flush();
				//PackageSchemes.printOut(scheme,out.toByteArray());
				try(Decoder decoder=scheme.createDecoder(context, out.toByteArray())) {
					Object decoded=decoder.decode(Object.class);
					try(ByteArrayOutputStream out2=new ByteArrayOutputStream();
							Encoder encoder2=scheme.createEncoder(context, out2)) {
						encoder2.encode(decoded).flush();
						assertArrayEquals(out.toByteArray(), out2.toByteArray());
					}
				}
			}
			System.gc();
		}
	}
	
	@Test
	public void testYaml() throws Throwable {
		//XMLCodingScheme scheme=XMLCodingScheme.builder().setIndent("  ","\r\n").setRootTag("root").build();//new XMLCodingScheme();
		Context context=Context.createRootContext(new DefaultTypeLoader());
		ObjectType.createClassType(PrimitiveTypeDefinition.class);
		for(int i=0;i<10;i++) {
			CodingScheme scheme=new YamlCodingScheme(new Yaml(Mode.Indented)).newBuilder().setRootType(Object.class).build();
			for(Object o:new Object[] {new not.alexa.coding.Data("Hello World",100,"T1","T2"),
					/*"\"Hello&World\"",Data.State.active,
					1,
					ObjectType.createClassType(PrimitiveTypeDefinition.class),
					ObjectType.resolve("oid:2.3.1.24.2")*/}) try(ByteArrayOutputStream out=new ByteArrayOutputStream();
				Encoder encoder=scheme.createEncoder(context, out)) {
				encoder.encode(o).flush();
				//PackageSchemes.printOut(scheme,out.toByteArray());
				try(Decoder decoder=scheme.createDecoder(context, out.toByteArray())) {
					Object decoded=decoder.decode(Object.class);
					try(ByteArrayOutputStream out2=new ByteArrayOutputStream();
							Encoder encoder2=scheme.createEncoder(context, out2)) {
						encoder2.encode(decoded).flush();
						assertArrayEquals(out.toByteArray(), out2.toByteArray());
					}
				}
			}
			System.gc();
		}
	}
	
	@Test
	public void testProtobuf() throws Throwable {
		//XMLCodingScheme scheme=XMLCodingScheme.builder().setIndent("  ","\r\n").setRootTag("root").build();//new XMLCodingScheme();
		Context context=Context.createRootContext(new DefaultTypeLoader());
		ObjectType.createClassType(PrimitiveTypeDefinition.class);
		for(int i=0;i<10;i++) {
			CodingScheme scheme=new ProtobufCodingScheme().newBuilder().setRootType(Object.class).build();
			for(Object o:new Object[] {new not.alexa.coding.Data("Hello World",100,"T1","T2"),
					/*"\"Hello&World\"",Data.State.active,
					1,
					ObjectType.createClassType(PrimitiveTypeDefinition.class),
					ObjectType.resolve("oid:2.3.1.24.2")*/}) try(ByteArrayOutputStream out=new ByteArrayOutputStream();
				Encoder encoder=scheme.createEncoder(context, out)) {
				encoder.encode(o).flush();
				//PackageSchemes.printOut(scheme,out.toByteArray());
				try(Decoder decoder=scheme.createDecoder(context, out.toByteArray())) {
					Object decoded=decoder.decode(Object.class);
					try(ByteArrayOutputStream out2=new ByteArrayOutputStream();
							Encoder encoder2=scheme.createEncoder(context, out2)) {
						encoder2.encode(decoded).flush();
						assertArrayEquals(out.toByteArray(), out2.toByteArray());
					}
				}
			}
			System.gc();
		}
	}
}
