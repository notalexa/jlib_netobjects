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
package not.alexa.netobjects.coding;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.text.IntegerCodec;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.EnumTypeDefinition;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.MethodTypeDefinition;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessFactory;

@RunWith(org.junit.runners.Parameterized.class)
public class AbstractTextCodingSchemeCodecsTest {
    private static AbstractTextCodingSchemeTest.DummyAccessFactory ACCESS=new AbstractTextCodingSchemeTest.DummyAccessFactory();

	@Parameters
	public static List<TestCase> primitiveClasses() {
		return Arrays.asList(new TestCase[] {
                new TestCase(TestScheme.DEFAULT_INSTANCE,TestScheme.NULL_INSTANCE,false,PrimitiveTypeDefinition.getTypeDescription(String.class),null),
		        new TestCase(TestScheme.DEFAULT_INSTANCE,TestScheme.NULL_INSTANCE,false,PrimitiveTypeDefinition.getTypeDescription(String.class),ACCESS),
                new TestCase(TestScheme.DEFAULT_INSTANCE,TestScheme.NULL_INSTANCE,false,PrimitiveTypeDefinition.getTypeDescription(Integer.class),ACCESS),
                new TestCase(TestScheme.DEFAULT_INSTANCE,TestScheme.NULL_INSTANCE,false,new EnumTypeDefinition(Flavour.class),ACCESS),
                new TestCase(TestScheme.DEFAULT_INSTANCE,TestScheme.NULL_INSTANCE,true,new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class)),ACCESS),
                new TestCase(TestScheme.DEFAULT_INSTANCE,TestScheme.NULL_INSTANCE,true,new MethodTypeDefinition(),ACCESS),
                new TestCase(TestScheme.DEFAULT_INSTANCE,TestScheme.NULL_INSTANCE,true,PrimitiveTypeDefinition.getTypeDescription(Object.class),ACCESS),
                
                new TestCase(TestScheme.DEFAULT_INSTANCE.newBuilder().addCodec(PrimitiveTypeDefinition.getTypeDescription(Integer.class).getJavaClassType(), new IntegerCodec(16)).build(),
                        TestScheme.DEFAULT_INSTANCE.newBuilder().addCodec(PrimitiveTypeDefinition.getTypeDescription(Integer.class).getJavaClassType(), new IntegerCodec(16)).build(),false,PrimitiveTypeDefinition.getTypeDescription(Integer.class),ACCESS)/*,
                new TestCase(TestScheme.DEFAULT_INSTANCE.newBuilder().addCodec(ObjectType.createClassType(Flavour.class), new IntegerCodec(16)).build(),PrimitiveTypeDefinition.getTypeDescription(Integer.class),ACCESS),
                new TestCase(TestScheme.DEFAULT_INSTANCE.newBuilder()
                        .setCharset("ISO-8859-1").build(),"class","obj-ref","obj-id","","",PrimitiveTypeDefinition.getTypeDescription(Object.class),Charset.forName("ISO-8859-1"),"mimetype","dat",ACCESS),
                new TestCase(TestScheme.DEFAULT_INSTANCE,"class","obj-ref","obj-id","","",PrimitiveTypeDefinition.getTypeDescription(Object.class),Charset.forName("UTF-8"),"mimetype","dat",ACCESS)*/
		});
	}
	
	@Parameter
	public TestCase testCase;
	
	public AbstractTextCodingSchemeCodecsTest() {
	}
	
	@Test
	public void checkAttribute1() {
	    try {
	        Codec codec=testCase.scheme1.getCodec(testCase.context, testCase.type,testCase.access);
	        System.out.println("Test1: Codec="+codec+" for "+testCase.type);
	    } catch(Throwable t) {
	        if(!testCase.expectThrowable) {
	            t.printStackTrace(); 
	        }
	    }
	}

	   @Test
	    public void checkAttribute2() {
	        try {
	            Codec codec=testCase.scheme2.getCodec(testCase.context, testCase.type,testCase.access);
	            System.out.println("Test2: Codec="+codec+" for "+testCase.type);
	        } catch(Throwable t) {
	            t.printStackTrace();
	        }
	    }

	private static class TestCase {
	    TestScheme scheme1;
        TestScheme scheme2;
        boolean expectThrowable;
	    Context context;
	    ObjectType type;
	    Access access;
	    public TestCase(TestScheme scheme1,TestScheme scheme2,boolean expectThrowable,TypeDefinition rootType,AccessFactory accessFactory) {
	        this.scheme1=scheme1;
            this.scheme2=scheme2;
            this.expectThrowable=expectThrowable;
	        this.context=context;
	        this.type=rootType.getJavaClassType();
	        this.access=accessFactory==null?null:accessFactory.resolve(context,rootType);
	    }
	}
	
	public static class TestScheme extends AbstractTextCodingScheme {
	    public static final TestScheme DEFAULT_INSTANCE=new TestScheme(Charset.forName("UTF-8"),ACCESS);
        public static final TestScheme NULL_INSTANCE=new TestScheme(true,Charset.forName("UTF-8"),ACCESS);
        private boolean walkThrough;
	    
        public TestScheme(Charset charset, AccessFactory factory) {
            this(false,charset,factory);
        }
	    public TestScheme(boolean walkThrough,Charset charset, AccessFactory factory) {
            super(charset, factory);
            this.walkThrough=walkThrough;
            mimeType="mimetype";
            fileExtension="dat";
        }

        public Builder newBuilder() {
	        return new Builder(this);
	    }

        
        @Override
        protected Codec createInterfaceCodec(Context context, ObjectType type, TypeDefinition typeDef) throws BaseException {
            return walkThrough?null:super.createInterfaceCodec(context, type, typeDef);
        }

        @Override
        protected Codec createMethodCodec(Context context, ObjectType type, TypeDefinition typeDef) throws BaseException {
            return walkThrough?null:super.createMethodCodec(context, type, typeDef);
        }

        @Override
        protected Codec createArrayCodec(Context context, ObjectType type, TypeDefinition typeDef) throws BaseException {
            return walkThrough?null:super.createArrayCodec(context, type, typeDef);
        }

        @Override
        public Encoder createEncoder(Context context, OutputStream stream) {
            return null;
        }

        @Override
        public Decoder createDecoder(Context context, InputStream stream) {
            return null;
        }
	    
        public static class Builder extends AbstractTextCodingScheme.Builder<TestScheme, Builder> {

            public Builder(TestScheme scheme) {
                super(scheme);
            }

            @Override
            public Builder myself() {
                return this;
            }
            
        }

        @Override
        public Codec getCodec(Context context, ObjectType type, Access access) throws BaseException {
            return super.getCodec(context, type, access);
        }
 	}

}
