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
package not.alexa.netobjects.types.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.access.RuntimeInfo.FieldMapper;
import not.alexa.netobjects.types.access.RuntimeInfo.Provider;
import not.alexa.netobjects.utils.Sequence;
import not.alexa.netobjects.utils.SimpleFieldMapper;

public class ProviderTest {
	
	public ProviderTest() {
	}
	
	@Test
	public void defaultProviderTest1() {
		Provider provider=new RuntimeInfo.DefaultProvider(T1.class, null, FieldMapper.IDENTITY);
		RuntimeInfo.addProvider(provider);
		LinkedLocal linkedLocal=ObjectType.createClassType(T1.class).asLinkedLocal(T1.class.getClassLoader());
		RuntimeInfo runtimeInfo=provider.resolve(linkedLocal, linkedLocal);
	}
	
	@Test
	public void defaultProviderTest2() {
		Provider provider=new RuntimeInfo.DeferredProvider(T2.class, Collections.emptyList(),null,FieldMapper.IDENTITY) {

			@Override
			protected java.lang.reflect.Constructor<?> findConstructor(Class<?> enclosingClass, Class<?> clazz) throws Throwable {
				throw new Throwable();
			}
			
		};
		LinkedLocal linkedLocal=ObjectType.createClassType(T2.class).asLinkedLocal(T2.class.getClassLoader());
		RuntimeInfo runtimeInfo=provider.resolve(linkedLocal, linkedLocal);
	}

	@Test
	public void defaultProviderTest3() {
		Provider provider=new RuntimeInfo.DeferredProvider(T3.class, Collections.singletonList("property"),null,FieldMapper.IDENTITY) {

			@Override
			protected java.lang.reflect.Constructor<?> findConstructor(Class<?> enclosingClass, Class<?> clazz) throws Throwable {
				return T3.class.getConstructor(String.class);
			}
			
		};
		LinkedLocal linkedLocal=ObjectType.createClassType(T3.class).asLinkedLocal(T3.class.getClassLoader());
		RuntimeInfo runtimeInfo=provider.resolve(linkedLocal, linkedLocal);
        Context context=Context.createRootContext(new DefaultTypeLoader());
        try(Sequence<Object> seq=YamlCodingScheme.DEFAULT_SCHEME.createDecoder(context, "class: not.alexa.netobjects.types.access.ProviderTest$T3\nproperty: Test".getBytes()).decodeAll(Object.class)) {
        	for(Object o:seq) {
        		System.out.println(o);
        	}
        } catch(BaseException e) {
        	//e.printStackTrace();
        }
	}

	@Test
	public void defaultProviderTest4() {
		Provider provider=new RuntimeInfo.DeferredProvider(T4.class, Collections.singletonList("property"),null,FieldMapper.IDENTITY) {

			@Override
			protected java.lang.reflect.Constructor<?> findConstructor(Class<?> enclosingClass, Class<?> clazz) throws Throwable {
				return clazz.getConstructor(String.class);
			}
		};
		RuntimeInfo.addProvider(provider);
		provider=new RuntimeInfo.DeferredProvider(T4.Inner.class,Arrays.asList("property","count"),null,new SimpleFieldMapper(Collections.singletonMap(T4.Inner.class.getName()+"#property", "prop1"))) {
			@Override
			protected java.lang.reflect.Constructor<?> findConstructor(Class<?> enclosingClass, Class<?> clazz)
					throws Throwable {
				return T4.Inner.class.getConstructor(enclosingClass,String.class,Integer.TYPE);
			}
		};
		RuntimeInfo.addProvider(provider);
		LinkedLocal linkedLocal=ObjectType.createClassType(T4.class).asLinkedLocal(T4.class.getClassLoader());
		RuntimeInfo runtimeInfo=provider.resolve(linkedLocal, linkedLocal);
		CodingScheme scheme=YamlCodingScheme.DEFAULT_SCHEME;
        Context context=Context.createRootContext(new DefaultTypeLoader());
        Context upcastContext1=Context.createRootContext(context.getTypeLoader().overlay(T4Overlay.class));
        Context upcastContext2=Context.createRootContext(context.getTypeLoader().overlay(T5Overlay.class));
        try(Sequence<T4> seq=scheme.createDecoder(context, "class: not.alexa.netobjects.types.access.ProviderTest$T4\nproperty: Test\ninner:\n  deferred: Deferred\n  property: Test\n  count: 100".getBytes()).decodeAll(T4.class)) {
        	for(T4 o:seq) {
        		System.out.println(o);
        		o.a=new T4[] { new T4("Array") };
        		o.l=Collections.singletonList(new T4("List"));
        		o.m=Collections.singletonMap("key",new T4("Map"));
        		o.t5=new T5(new T4("T5"));//new T5("T5");
        		System.out.println(upcastContext1.upcast(o));
        		System.out.println(upcastContext2.upcast(o));
        	}
        } catch(BaseException e) {
        	e.printStackTrace();
        }

	}

	@Test
	public void defaultProviderTest5() {
		Provider provider=new RuntimeInfo.DeferredProvider(T4.class, Collections.singletonList("property"),null,FieldMapper.IDENTITY) {

			@Override
			protected java.lang.reflect.Constructor<?> findConstructor(Class<?> enclosingClass, Class<?> clazz) throws Throwable {
				return clazz.getConstructor(String.class);
			}
		};
		RuntimeInfo.addProvider(provider);
		provider=new RuntimeInfo.DeferredProvider(T4.Inner.class,Arrays.asList("property","count"),null,new SimpleFieldMapper(Collections.singletonMap(T4.Inner.class.getName()+"#property", "prop1"))) {
			@Override
			protected java.lang.reflect.Constructor<?> findConstructor(Class<?> enclosingClass, Class<?> clazz)
					throws Throwable {
				return T4.Inner.class.getConstructor(enclosingClass,String.class,Integer.TYPE);
			}
		};
		RuntimeInfo.addProvider(provider);
		LinkedLocal linkedLocal=ObjectType.createClassType(T4.class).asLinkedLocal(T4.class.getClassLoader());
		RuntimeInfo runtimeInfo=provider.resolve(linkedLocal, linkedLocal);
		CodingScheme scheme=YamlCodingScheme.DEFAULT_SCHEME;
        Context context=Context.createRootContext(new DefaultTypeLoader().overlay(T4Overlay.class));
        try(Sequence<Object> seq=scheme.createDecoder(context, "class: not.alexa.netobjects.types.access.ProviderTest$T4\nproperty: Test\ninner:\n  deferred: Deferred\n  property: Test\n  count: 100".getBytes()).decodeAll(Object.class)) {
        	for(Object o:seq) {
        		System.out.println(o);
        	}
        } catch(BaseException e) {
        	e.printStackTrace();
        }

	}

	public static class T1 {
		
	}

	public static class T2 {
		
	}

	public static class T3 {
		private static ClassTypeDefinition DESCR=new ClassTypeDefinition(T3.class);
		static {
			DESCR.createBuilder()
				.createField("property", PrimitiveTypeDefinition.getTypeDescription(String.class)).build()
				.build();
		}
		
		public static TypeDefinition getTypeDescription() {
			return DESCR;
		}
		
		protected String property;
	}


	public static class T4 {
		private static ClassTypeDefinition DESCR=new ClassTypeDefinition(T4.class);
		static {
			DESCR.createBuilder()
				.createField("property", PrimitiveTypeDefinition.getTypeDescription(String.class)).build()
				.createField("a", new ArrayTypeDefinition(DESCR)).setOptional(true).build()
				.createField("l", new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(Object.class))).setOptional(true).build()
				.createField("m",new ArrayTypeDefinition(new ClassTypeDefinition().createBuilder()
					.createField("k", PrimitiveTypeDefinition.getTypeDescription(String.class))
					    .addTag("XML","@k").setOptional(true).build()
					.addField("v", PrimitiveTypeDefinition.getTypeDescription(Object.class))
					.build())).setOptional(true).build()
				.createField("inner",new ClassTypeDefinition(T4.Inner.class).createBuilder()
						.createField("deferred", PrimitiveTypeDefinition.getTypeDescription(String.class)).build()
						.createField("property", PrimitiveTypeDefinition.getTypeDescription(String.class)).build()
						.createField("count", PrimitiveTypeDefinition.getTypeDescription(Integer.class)).build()
						.build()).build()
				.createField("t5",new ClassTypeDefinition(T5.class).createBuilder()
						.createField("obj", PrimitiveTypeDefinition.getTypeDescription(Object.class)).build()
						.build()).setOptional(true).build()
				.build();
		}
		
		public static TypeDefinition getTypeDescription() {
			return DESCR;
		}
		
		protected String property;
		protected Inner inner;
		protected T5 t5;
		protected T4[] a;
		protected List<Object> l;
		protected Map<String,Object> m;

		public T4(String property) {
			this.property=property;
		}
		
		protected Object finish(AccessContext context) {
			return this;
		}
		
		public class Inner {
			protected String deferred;
			protected String prop1;
			protected int count;
			
			public Inner(String property,int count) {
				this.prop1=property;
				this.count=count;
			}
		}
	}
	
	public static class T5 {
		Object obj;
		T5() {
			
		}
		T5(Object obj) {
			this.obj=obj;
		}
	}
	
	@Overlay
	public static class T4Overlay extends T4 {

		public T4Overlay(String property) {
			super(property);
		}
		
	}
	
	@Overlay
	public static class T5Overlay extends T5 {
		private T5Overlay() {
			
		}
		public T5Overlay(Object obj) {
			super(obj);
		}
		
	}

}
