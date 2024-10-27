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

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.JavaClass;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.access.FieldAccessor.Getter;
import not.alexa.netobjects.types.access.FieldAccessor.Setter;
import not.alexa.netobjects.types.access.ReflectionClassAccess.Resolver;
import not.alexa.netobjects.types.access.RuntimeInfo.Provider;

/**
 * This class and it's inner classes are not intended for direct usage.
 * 
 * @author notalexa
 *
 */
class RuntimeInfoHelper {
	static ReferenceQueue<Class<?>> QUEUE=new ReferenceQueue<>();
	static private Map<String,Provider> refs=new HashMap<>();
	static private Map<Class<?>,CodingFilter> filters=new HashMap<Class<?>, CodingFilter>();
	
	static {
		addFilter(new FileFilter());
	}

	static final RuntimeInfo TRIVIAL_RUNTIME_INFO=new RuntimeInfo() {

		@Override
		public PreAccessible newInstance(AccessContext context) throws BaseException {
			return new PreAccessible() {
				
				@Override
				public AccessibleObject makeAccessible(Access access) throws BaseException {
					return access.newAccessible(context);
				}
			};
		}

		@Override
		public String mapField(Class<?> clazz, String fieldName) {
			return fieldName;
		}
		
		@Override
		public FieldAccessor createFieldAccess(Resolver resolver, Class<?> clazz, Field f) {
			return null;
		}
	};

	
	static void add(Provider provider) {
		Provider releaseRef;			
		while((releaseRef=(Provider)QUEUE.poll())!=null) {
			Provider start=refs.get(releaseRef.clazzName);
			//System.out.println("Remove ref "+releaseRef.clazzName);
			if(start==releaseRef) {
				if(start.next==null) {
					refs.remove(releaseRef.clazzName);
				} else {
					refs.put(releaseRef.clazzName, start.next);
				}
			} else {
				while(start.next!=null&&start.next!=releaseRef) {
					start=start.next;
				}
				if(start.next!=null) {
					start.next=start.next.next;
				}
			}
		}
		Class<?> constructorClass=provider.get();
		Provider first=refs.get(provider.clazzName);
		Provider rover=first;
		while(rover!=null&&!constructorClass.equals(rover.get())) {
			rover=rover.next;
		}
		if(rover==null) {
			provider.next=first;
			refs.put(provider.clazzName,provider);
		}
	}
	
	static RuntimeInfo get(LinkedLocal clazz,LinkedLocal overlay) {
		if(!clazz.isClass()) {
			return null;
		} else {
			Class<?> c=clazz.asClass();
			Provider ref=refs.get(c.getName());
			while(ref!=null) {
				Class<?> constructor=ref.get();
				if(constructor!=null&&c.equals(constructor)) {
					return ref.resolve(clazz,overlay);
				}
				ref=ref.next;
			}
			return new DefaultRuntimeInfo(c,overlay);
		}
	}
		
	static abstract class AbstractRuntimeInfo implements RuntimeInfo {
        protected Class<?> clazz;
        protected Class<?> enclosingClass;
        protected Method finish;
        protected java.lang.reflect.Constructor<?> constructor;
        protected Throwable initializerException;
        protected FieldMapper fieldMap;
        protected Injector[] injectors;
        
        public AbstractRuntimeInfo(LinkedLocal linkedLocal,InjectorInfos injectorInfos,FieldMapper fieldMap) {
            this.clazz=linkedLocal.asClass();
            this.fieldMap=fieldMap;
            enclosingClass=RuntimeInfo.getEnclosingClass(clazz);
            injectors=injectorInfos==null?null:injectorInfos.getInjectors(clazz);
            init(clazz);
        }
        
        protected void init(Class<?> clazz) {
            if(clazz!=null) {
                if(finish==null) try {
                    finish=clazz.getDeclaredMethod("finish",AccessContext.class);
                    finish.setAccessible(true);
                } catch(Throwable t) {
                }
                init(clazz.getSuperclass());
            }
        }
        
        protected Object getEnclosingInstance(AccessContext context) throws BaseException {
        	if(enclosingClass==null) {
        		return null;
        	} else {
        		Object o=context.castTo(enclosingClass);
        		if(o==null) {
                    throw new BaseException(BaseException.NOT_FOUND,"Enclosing instance of type "+enclosingClass.getName()+" is missing to construct an instanceof of "+clazz.getName());
        		} else {
        			return o;
        		}
        	}
        }
        
		@Override
		public final String mapField(Class<?> clazz,String fieldName) {
			return fieldMap.mapField(clazz, fieldName);
		}
		
		protected abstract class Accessible extends AbstractAccessibleObject {
			private Object finished;
			public Accessible(Access access) {
				super(access);
			}
			
			protected abstract AccessContext getFinishContext();
			
            @Override
            public Object getAssignable(AccessContext context) throws BaseException {
            	if(finished==null) {
            		Object o=getObject();
            		if(o==null) {
            			throw new BaseException(BaseException.FORBIDDEN,"Missing fields.");
            		}
            		try {
            			finished=finish==null?o:finish.invoke(o,getFinishContext());
            		} catch(Throwable t) {
            			return BaseException.throwException(t);
            		}
            	}
                return access.finish(finished);
            }
		}
	}
	
	static interface Injector {
		public void inject(AccessContext context,Object o) throws BaseException;
	}
	
	public static int methodPrio(int type,Method m) {
		if(m.getParameterCount()==type) {
			return 1;
		} else if(m.getParameterCount()==type+1) {
			if(Context.class.isAssignableFrom(m.getParameterTypes()[0])) {
				return 2+0x200;
			} else if(AccessContext.class.isAssignableFrom(m.getParameterTypes()[0])) {
				return 3+0x200;
			}
		}
		return -1;
	}

    public static FieldAccessor createDefaultFieldAccess(Resolver resolver,Class<?> clazz,Field f) {
    	JavaClass.Type type=f.getType().getJavaClassType();
    	if(f.getType().getFlavour()==Flavour.ArrayType) {
    		type=null;
    	}
    	java.lang.reflect.Field field=null;
    	String s=Character.toUpperCase(f.getName().charAt(0))+f.getName().substring(1);
    	Set<String> getterNames=new HashSet<String>();
    	Set<String> setterNames=new HashSet<String>();
    	String name=clazz.getName()+"."+f.getName();
    	getterNames.add(f.getName());
    	getterNames.add("get"+s);
    	getterNames.add("is"+s);
    	setterNames.add(f.getName());
    	setterNames.add("set"+s);
    	Method getter=null;
    	List<Method> setters=new ArrayList<Method>();
        while(clazz!=null) try {
        	for(Method m:clazz.getDeclaredMethods()) {
        		if(getterNames.contains(m.getName())&&methodPrio(0,m)>=0) {
        			if(getter==null||methodPrio(0,getter)<methodPrio(0,m)) {
        				if(type==null||type.equals(resolver.resolve(m).asType())) {
        					getter=m;
        				}
        			}
        		}
        		if(setterNames.contains(m.getName())&&methodPrio(1,m)>=0) {
        			setters.add(m);
        		}
        	}
            java.lang.reflect.Field f0=clazz.getDeclaredField(resolver.mapField(clazz,f.getName()));
			if(type==null||type.equals(resolver.resolve(f0).asType())) {
				f0.setAccessible(true);
				field=f0;
			}
        } catch(Throwable t) {
        } finally {
        	clazz=clazz.getSuperclass();
        }
        if(field==null&&getter==null) {
        	return null;
        }
        Type fieldType=getter!=null?getter.getGenericReturnType():field.getGenericType();
        Method setter=null;
        for(Method m:setters) {
        	if(fieldType.equals(m.getGenericParameterTypes()[m.getParameterCount()-1])) {
        		if(setter==null||methodPrio(1, m)>methodPrio(1,setter)) {
        			setter=m;
        		}
        	}
        }
        return new FieldAccessor(name,getter==null?resolver.resolve(field):resolver.resolve(getter),field,getter,setter);
    }

	
	static class DefaultRuntimeInfo extends AbstractRuntimeInfo {

		public DefaultRuntimeInfo(Class<?> clazz,LinkedLocal linkedLocal) {
			this(linkedLocal,null,FieldMapper.IDENTITY);
		}

		public DefaultRuntimeInfo(LinkedLocal linkedLocal, InjectorInfos injectorInfos,FieldMapper fieldMap) {
			super(linkedLocal, injectorInfos, fieldMap);
            try {
                constructor=enclosingClass==null?clazz.getDeclaredConstructor():clazz.getDeclaredConstructor(enclosingClass);
                constructor.setAccessible(true);
            } catch(Throwable t) {
                initializerException=t;
            }
		}

		public DefaultRuntimeInfo(LinkedLocal linkedLocal,Throwable error) {
			super(linkedLocal,null,FieldMapper.IDENTITY);
			initializerException=error;
		}
		
	    public FieldAccessor createFieldAccess(Resolver resolver,Class<?> clazz,Field f) {
	    	return createDefaultFieldAccess(resolver, clazz, f);
	    }


		@Override
		public PreAccessible newInstance(AccessContext context) throws BaseException {
			try {
				if(null!=initializerException) {
					throw initializerException;
				}
				Object enclosingInstance=getEnclosingInstance(context);
				Object o=enclosingInstance==null?constructor.newInstance():constructor.newInstance(enclosingInstance);
				if(injectors!=null) for(Injector injector:injectors) {
					injector.inject(context,o);
				}
				return new PreAccessible() {
					@Override
					public AccessibleObject makeAccessible(Access access) {
						return new Accessible(access) {
							@Override
							public Object getObject() {
								return o;
							}

							@Override
							protected AccessContext getFinishContext() {
								return context;
							}
						};
					}
				};
			} catch(Throwable t) {
				return BaseException.throwException(t);
			}
		}
	}
		
	static class DeferredRuntimeInfo extends AbstractRuntimeInfo {
		private List<String> constructorFields;
		private Set<String> optional;
		private Initializer initializer;
		private class Initializer {
			private InjectorInfo[] infos=new InjectorInfo[constructor.getParameterCount()];
			public Initializer(InjectorInfos injectorInfos) {
				int offset=enclosingClass==null?0:1;
				for(int i=offset;i<infos.length;i++) {
					infos[i]=injectorInfos==null?null:injectorInfos.getParameterInfo(i-offset);
				}
			}
			public Object[] initialize(AccessContext context,Access access,Set<String> optional,Map<String,Value> values) throws BaseException {
				Object[] args=new Object[infos.length];
				int offset=enclosingClass==null?0:1;
				int fieldIndex=0;
				for(int i=offset;i<infos.length;i++) {
					if(infos[i]!=null) {
						args[i]=infos[i].get(context, constructor.getParameterTypes()[i]);
					} else {
						Value v=values.remove(constructorFields.get(i-fieldIndex));
						if(v!=null) {
							args[i]=access.getFieldAccess(v.f).getObject(context, v.val);
						} else if(!optional.contains(constructorFields.get(i-offset))) {
							// Just keep o unset.
							return null;
						}
					}
				}
				if(offset>0) {
					args[0]=getEnclosingInstance(context);
				}
				return args;
			}
		}
		class Value {
			Field f;
			AccessibleObject val;
			Value(Field f,AccessibleObject val) {
				this.f=f;
				this.val=val;
			}
		}

		public DeferredRuntimeInfo(LinkedLocal linkedLocal, java.lang.reflect.Constructor<?> constructor,List<String> constructorFields, InjectorInfos injectorInfos,FieldMapper fieldMap) {
			super(linkedLocal, injectorInfos,fieldMap);
			this.constructor=constructor;
			this.constructorFields=constructorFields;
			this.initializer=new Initializer(injectorInfos);
		}
		
		private synchronized Set<String> getOptionalConstructorFields(Access access) {
			if(optional==null) {
				optional=new HashSet<>();
				for(Field f:((ClassTypeDefinition)access.getType()).getFields()) {
					if(f.isOptional()&&constructorFields.contains(f.getName())) {
						optional.add(f.getName());
					}
				}
			}
			return optional;
		}
		
		@Override
		public FieldAccessor createFieldAccess(Resolver resolver, Class<?> clazz, Field f) {
			if(Object.class.equals(clazz)&&constructorFields.contains(mapField(clazz, f.getName()))) {
				try {
					String s=mapField(clazz,f.getName());
					Method m=resolver.clazz.getMethod("get"+Character.toUpperCase(s.charAt(0))+s.substring(1));
					return new FieldAccessor(clazz.getName()+"."+f.getName(),resolver.resolve(m.getReturnType()), null,m,null);
				} catch(Throwable t) {
				}
				return null;
			} else {
				return createDefaultFieldAccess(resolver, clazz, f);
			}
		}


		@Override
		public PreAccessible newInstance(AccessContext context) throws BaseException {
			try {
				if(initializerException!=null) {
					throw initializerException;
				}
	        	Set<String> fields=new HashSet<>(this.constructorFields);
	            Map<String,Value> values=new LinkedHashMap<>();
				return new PreAccessible() {
					@Override
					public AccessibleObject makeAccessible(Access access) {
						return new Accessible(access) {
							Object o;
							{
								if(fields.size()==0) try {
									create(Collections.emptySet());
								} catch(Throwable t) {
									t.printStackTrace();
								}
							}
							
							private void create(Set<String> optional) throws Throwable {
								Object[] args=initializer.initialize(context, access,optional,values);
								if(args!=null) {
									o=DeferredRuntimeInfo.this.constructor.newInstance(args);
									if(injectors!=null) for(Injector injector:injectors) {
										injector.inject(context,o);
									}
									if(values.size()>0) for(Map.Entry<String, Value> entry:values.entrySet()) {
										super.setField(context,entry.getValue().f,entry.getValue().val);
									}
									values.clear();
								}
							}
							
							@Override
							public Object getObject() {
								if(o==null&&fields.size()>0) try {
									create(getOptionalConstructorFields(access));
								} catch(Throwable t) {
									// unset o will cause an error
								}
								return o;
							}
							
							@Override
							protected AccessContext getFinishContext() {
								return context;
							}

							@Override
							public void setField(AccessContext context,Field f, not.alexa.netobjects.types.AccessibleObject v) throws BaseException {
								if(o==null) {
									values.put(f.getName(), new Value(f,v));
									if(fields.remove(f.getName())) {
										if(fields.size()==0) try {
											create(Collections.emptySet());
										} catch(Throwable t) {
											BaseException.throwException(t);
										}
									}
								} else if(fields.size()!=0&&fields.contains(f.getName())) {
									throw new BaseException(BaseException.FORBIDDEN,"Constructor field "+f.getName()+" set after object creation.");
								} else {
									super.setField(context,f, v);
								}
							}

							@Override
							public not.alexa.netobjects.types.AccessibleObject getField(AccessContext context,Field f) throws BaseException {
								if(o==null) {
									throw new BaseException(BaseException.FORBIDDEN,"Illegal access to field "+f.getName()+" of deferred object.");
								}
								return super.getField(context,f);
							}
						};
					}
				};
			} catch(Throwable t) {
				return BaseException.throwException(t);
			}
		}
	}
	
	public static CodingFilter getFilter(Class<?> clazz) {
		return filters.get(clazz);
	}
	
	public static void addFilter(CodingFilter filter) {
		filters.put(filter.getSourceClass(), filter);
	}
	
	public static class FileFilter implements CodingFilter {

		@Override
		public Getter filter(Getter getter) {
			return new Getter() {
				
				@Override
				public Object invoke(AccessContext context, Object o) throws Throwable {
					Object v=getter.invoke(context, o);
					return v==null?null:((File)v).getPath();
				}
			};
		}

		public Class<?> getCodingClass() {
			return String.class;
		}

		@Override
		public Setter filter(Setter setter) {
			return new Setter() {
				@Override
				public void invoke(AccessContext context, Object o, Object v) throws Throwable {
					setter.invoke(context, o, v==null?null:new File((String)v));
				}
			};
		}

		@Override
		public Class<?> getSourceClass() {
			return File.class;
		}

		@Override
		public void install() {
			addFilter(this);
		}		
	}
}
