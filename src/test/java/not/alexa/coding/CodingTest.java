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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import not.alexa.coding.Data.State;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;

@RunWith(org.junit.runners.Parameterized.class)
public class CodingTest {

	public CodingTest() {
	}
	
    @Parameters
    public static List<CodingScheme> testSchemata() {
    	return Arrays.asList(PackageSchemes.SCHEMATA);
    }
    
    @Parameter
    public CodingScheme scheme;

	@Test
	public void test() throws Throwable {
		//XMLCodingScheme scheme=XMLCodingScheme.builder().setIndent("  ","\r\n").setRootTag("root").build();//new XMLCodingScheme();
		Context context=Context.createRootContext(new DefaultTypeLoader());
		ObjectType.createClassType(PrimitiveTypeDefinition.class);
		for(Object o:new Object[] {new not.alexa.coding.Data("Hello World",100,"T1","T2"),
				/*"\"Hello&World\"",Data.State.active,
				1,
				ObjectType.createClassType(PrimitiveTypeDefinition.class),
				ObjectType.resolve("oid:2.3.1.24.2")*/}) try(ByteArrayOutputStream out=new ByteArrayOutputStream();
			Encoder encoder=scheme.createEncoder(context, out)) {
			encoder.encode(o).flush();
			System.out.write(out.toByteArray());System.out.println();
			try(Decoder decoder=scheme.createDecoder(context, out.toByteArray())) {
				Object decoded=decoder.decode(Object.class);
				System.out.println(decoded);
				try(ByteArrayOutputStream out2=new ByteArrayOutputStream();
						Encoder encoder2=scheme.createEncoder(context, out2)) {
					encoder2.encode(decoded).flush();
					System.out.write(out2.toByteArray());System.out.println();
				}
			}
		}
		System.out.println(State[].class.getCanonicalName());
		System.out.println(Byte.TYPE.getCanonicalName());
		System.out.println(String.class.getCanonicalName());
		System.out.println(String[][].class.getName());
	}
	
    @Test
    public void overlayTest() throws Throwable {
        //XMLCodingScheme scheme=XMLCodingScheme.builder().setIndent("  ","\r\n").setRootTag("root").build();//new XMLCodingScheme();
        DefaultTypeLoader resolver=new DefaultTypeLoader();
        Context context=Context.createRootContext(resolver);
        Context overlayContext=Context.createRootContext(resolver.overlay(DataOverlay.class));
        ObjectType.createClassType(PrimitiveTypeDefinition.class);
        for(Object o:new Object[] {new not.alexa.coding.Data("Hello World",100,"T1","T2")}) try(ByteArrayOutputStream out=new ByteArrayOutputStream();
            Encoder encoder=scheme.createEncoder(context, out)) {
            encoder.encode(o).flush();
            System.out.write(out.toByteArray());System.out.println();
            try(Decoder decoder=scheme.createDecoder(overlayContext, out.toByteArray())) {
                Object decoded=decoder.decode(Object.class);
                assertEquals(DataOverlay.class,decoded.getClass());
                System.out.println(decoded);
                try(ByteArrayOutputStream out2=new ByteArrayOutputStream();
                        Encoder encoder2=scheme.createEncoder(context, out2)) {
                    encoder2.encode(decoded).flush();
                    System.out.write(out2.toByteArray());System.out.println();
                }
            }
        }
    }
    
    @Overlay
    public static class DataOverlay extends Data {
    }
}
