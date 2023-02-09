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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Field;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.api.ResolvableBy;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.jackson.JacksonResolver;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader;
import not.alexa.netobjects.utils.TypeUtils.ClassResolver;

public class ClassResolverTest {

    public ClassResolverTest() {
    }

    @Test
    public void test() {
//        ClassResolver resolver=TypeUtils.createClassResolver(A.class);
//        for(Class<?> c=resolver.getRootClass();!c.equals(Object.class);c=c.getSuperclass()) {
//            for(java.lang.reflect.Field f:c.getDeclaredFields()) {
//                System.out.println(f.getName()+"->"+resolver.resolve(f.getGenericType()));
//            }
//        }
        JacksonResolver typeResolver=new JacksonResolver();
        DefaultTypeLoader loader=new DefaultTypeLoader();
        Context context=Context.createRootContext(loader);
        try {
            TypeDefinition type=typeResolver.resolve(loader, ObjectType.createClassType(A.class));
            byte[] serialized=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context).encode(type).asBytes();
            System.out.write(serialized);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }
    
    @Test
    public void writeOut() {
    	A a=new A("xxx",Collections.singletonList("yyy"));
        JacksonResolver typeResolver=new JacksonResolver();
        DefaultTypeLoader.addTypeResolver(typeResolver);
        DefaultTypeLoader loader=new DefaultTypeLoader();//.overlay(AOverlay.class);
        Context context=Context.createRootContext(loader);
        try {
            TypeDefinition type=typeResolver.resolve(loader, ObjectType.createClassType(A.class));
            byte[] serialized=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setIndent("  ","\n").build().createEncoder(context).encode(a).asBytes();
            System.out.write(serialized);
            System.out.println();
            String data="<object>\r\n"
            		+ "  <s class=\"java.lang.String\">xxx</s>\r\n"
            		+ "  <a1 class=\"java.lang.String\">yyy</a1>\r\n"
            		+ "</object>";
            Context overlayContext=Context.createRootContext(loader.overlay(AOverlay.class));
            System.out.println(overlayContext.getTypeLoader().getLinkedLocal(type).asClass());
            a=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setRootType(type).build().createDecoder(overlayContext, data.getBytes()).decode(A.class);
            System.out.println(a);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    	
    }
    
    public static class A0 extends @ResolvableBy("jackson") A {

        public A0(String s, List<String> a1) {
            super(s, a1);
        }
    }
// TODO Lis<Date> is WRONG!! Set object type Object.class (List is an interface?) Same with array...
    public static class A extends B<Date,Date[]> {
        @JsonProperty("s") String s;
        @JsonProperty("a1") List<String> a1;
        @JsonProperty("a2") List<? extends B<String,String>> a2;
        @JsonProperty("a3") Map<@Field(name="key-word") String,@Field(name="v") Map<@Field(name="k") String,Object>> a3;
        protected A() {}
        @JsonCreator
        public A(@JsonProperty("s") String s,@JsonProperty("a1") List<String> a1) {
            this.s=s;
            this.a1=a1;
            t=new Date();
            r=new Date[] {t,t};//new ArrayList<Date>(Collections.singleton(new Date()));
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
    	
    }
}
