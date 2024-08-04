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
package not.alexa.netobjects.jackson;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Field;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.api.ResolvableBy;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.utils.Sequence;

public class ClassResolverTest {

    public ClassResolverTest() {
    }

    @Test
    public void test() {
        Context context=Context.createRootContext();
        try {
            TypeDefinition type=context.getTypeLoader().resolveType(A.class);
            byte[] serialized=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context).encode(type).asBytes();
            System.out.write(serialized);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }
    
    @Test
    public void writeOut() {
    	A a=new A("xxx",Collections.singletonList("yyy"));
        Context context=Context.createRootContext();
        try {
            TypeDefinition type=context.getTypeLoader().resolveType(A.class);
            byte[] serialized=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context).encode(a).asBytes();
            System.out.write(serialized);
            System.out.println();
            String data="<object>\r\n"
            		+ "  <property>xxx</property>\r\n"
            		+ "  <a1>yyy</a1>\r\n"
            		+ "</object>";
            Context overlayContext=Context.createRootContext(context.getTypeLoader().overlay(AOverlay.class));
            System.out.println(overlayContext.getTypeLoader().getLinkedLocal(type).asClass());
            a=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setRootType(type).build().createDecoder(overlayContext, data.getBytes()).decode(A.class);
            System.out.println(a);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    	
    }

    @Test
    public void xmlTest() {
    	Context context=Context.createRootContext();
    	CodingScheme scheme=XMLCodingScheme.DEFAULT_SCHEME;//.newBuilder().setRootType(context.resolveType(Object[].class)).build();
    	try(Sequence<Object> seq=scheme.createDecoder(context, "<object class=\"not.alexa.netobjects.jackson.ClassResolverTest$F\"><o class=\"java.lang.Object[]\"><o class=\"java.lang.String\">Hello World</o><o class=\"int\">123</o></o></object>\n".getBytes()).decodeAll(Object.class)) {
    		for(Object o:seq) {
    			assert(((F)o).o.getClass().isArray());
    		}
    	} catch(BaseException t) {
    		t.printStackTrace();
    		fail();
    	}
    }
    
    @Test
    public void nullTest1() {
    	Context context=Context.createRootContext();
        String data="<object class=\"not.alexa.netobjects.jackson.ClassResolverTest$A\">\r\n"
        		+ "  <property>xxx</property>\r\n"
//        		+ "  <a1>yyy</a1>\r\n"
        		+ "</object>";
        try {
        	A a=XMLCodingScheme.DEFAULT_SCHEME.createDecoder(context, data.getBytes()).decode(A.class);
        	System.out.println(a);
        } catch(BaseException e) {
        	e.printStackTrace();
        	fail();
        }
    	
    }
    
    @Test
    public void nullTest2() {
    	Context context=Context.createRootContext();
        String data="<object class=\"not.alexa.netobjects.jackson.ClassResolverTest$A\">\r\n"
//        		+ "  <property>xxx</property>\r\n"
        		+ "  <a1>yyy</a1>\r\n"
        		+ "</object>";
        try {
        	A a=XMLCodingScheme.DEFAULT_SCHEME.createDecoder(context, data.getBytes()).decode(A.class);
        	System.out.println(a);
        } catch(BaseException e) {
        	e.printStackTrace();
        	fail();
        }
    	
    }
    
    @Test
    public void nullTest3() {
    	Context context=Context.createRootContext();
        String data="<object class=\"not.alexa.netobjects.jackson.ClassResolverTest$A\">\r\n"
//        		+ "  <property>xxx</property>\r\n"
//        		+ "  <a1>yyy</a1>\r\n"
        		+ "</object>";
        try {
        	A a=XMLCodingScheme.DEFAULT_SCHEME.createDecoder(context, data.getBytes()).decode(A.class);
        	System.out.println(a);
        } catch(BaseException e) {
        	e.printStackTrace();
        	fail();
        }
    	
    }

    public static class A0 extends @ResolvableBy("jackson") A {

        public A0(String s, List<String> a1) {
            super(s, a1);
        }
    }

    public static class A extends B<Date,Date[]> {
        @JsonProperty(value="property",defaultValue="test") String s;
        @JsonProperty("a1") List<String> a1;
        @JsonProperty("a2") List<? extends B<String,String>> a2;
        @JsonProperty("a3") Map<@Field(name="key-word") String,@Field(name="v") Map<@Field(name="k") String,Object>> a3;
        @JsonProperty A[] alternatives;
        
        @JsonCreator
        public A(@JsonProperty("property") String s,@JsonProperty("a1") List<String> a1) {
            this.s=s;
            this.a1=a1;
            t=new Date();
            r=new Date[] {t,t};
        }
        
    }
    
    public static class B<T,R> {
        @JsonProperty("t") T t;
        @JsonProperty("r") R r;
        @JsonProperty("t1") T[] t1;
        @JsonProperty("s1") String[] s1;
    }
    
    @Overlay
    public static class AOverlay extends A {
    	
        public AOverlay(@JsonProperty("property") String s,@JsonProperty("a1") List<String> a1) {
        	super(s,a1);
        }
    }
    
    public static class F {
    	@JsonProperty Object o;
    }
}
