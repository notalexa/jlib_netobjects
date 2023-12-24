package not.alexa.netobjects.types.access;

import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.access.Constructor.Provider;

/**
 * This class and it's inner classes are not intended for direct usage.
 * 
 * @author notalexa
 *
 */
class ConstructorHelper {
	static ReferenceQueue<Class<?>> QUEUE=new ReferenceQueue<>();
	static private Map<String,Provider> refs=new HashMap<>();
	static final Constructor TRIVIAL_CONSTRUCTOR=new Constructor() {

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
	};

	
	static void add(Provider provider) {
		Provider releaseRef;			
		while((releaseRef=(Provider)QUEUE.poll())!=null) {
			Provider start=refs.get(releaseRef.clazzName);
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
	
	static Constructor get(LinkedLocal clazz,LinkedLocal overlay) {
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
			return new DefaultConstructor(overlay);
		}
	}
	
	static abstract class AbstractConstructor implements Constructor {
        protected Class<?> clazz;
        protected Class<?> enclosingClass;
        protected Method finish;
        protected java.lang.reflect.Constructor<?> constructor;
        protected Throwable initializerException;
        protected FieldMapper fieldMap;
        
        public AbstractConstructor(LinkedLocal linkedLocal,FieldMapper fieldMap) {
            this.clazz=linkedLocal.asClass();
            this.fieldMap=fieldMap;
            enclosingClass=Constructor.getEnclosingClass(clazz);
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
            public Object getAssignable() throws BaseException {
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
	
	static class DefaultConstructor extends AbstractConstructor {

		public DefaultConstructor(LinkedLocal linkedLocal) {
			this(linkedLocal,FieldMapper.IDENTITY);
		}

		public DefaultConstructor(LinkedLocal linkedLocal, FieldMapper fieldMap) {
			super(linkedLocal, fieldMap);
            try {
                constructor=enclosingClass==null?clazz.getDeclaredConstructor():clazz.getDeclaredConstructor(enclosingClass);
                constructor.setAccessible(true);
            } catch(Throwable t) {
                initializerException=t;
            }
		}

		public DefaultConstructor(LinkedLocal linkedLocal,Throwable error) {
			super(linkedLocal,FieldMapper.IDENTITY);
			initializerException=error;
		}


		@Override
		public PreAccessible newInstance(AccessContext context) throws BaseException {
			try {
				if(null!=initializerException) {
					throw initializerException;
				}
				Object enclosingInstance=getEnclosingInstance(context);
				Object o=enclosingInstance==null?constructor.newInstance():constructor.newInstance(enclosingInstance);
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
		
	static class DeferredConstructor extends AbstractConstructor {
		List<String> constructorFields;
		
		public DeferredConstructor(LinkedLocal linkedLocal, java.lang.reflect.Constructor<?> constructor,List<String> constructorFields, FieldMapper fieldMap) {
			super(linkedLocal, fieldMap);
			this.constructor=constructor;
			this.constructorFields=constructorFields;
		}

		@Override
		public PreAccessible newInstance(AccessContext context) throws BaseException {
			try {
				if(initializerException!=null) {
					throw initializerException;
				}
				class Value {
					Field f;
					AccessibleObject val;
					Value(Field f,AccessibleObject val) {
						this.f=f;
						this.val=val;
					}
				}
	        	Set<String> fields=new HashSet<>(this.constructorFields);
	            Map<String,Value> values=new LinkedHashMap<>();
	            Object enclosingInstance=getEnclosingInstance(context);
				return new PreAccessible() {
					@Override
					public AccessibleObject makeAccessible(Access access) {
						return new Accessible(access) {
							Object o;
							@Override
							public Object getObject() {
								return o;
							}
							
							@Override
							protected AccessContext getFinishContext() {
								return context;
							}

							@Override
							public void setField(Field f, not.alexa.netobjects.types.AccessibleObject v) throws BaseException {
								if(o==null) {
									values.put(f.getName(), new Value(f,v));
									if(fields.remove(f.getName())) {
										if(fields.size()==0) try {
											int offset=enclosingClass==null?0:1;
											Object[] args=new Object[offset+constructorFields.size()];
											for(int i=offset;i<args.length;i++) {
												args[i]=values.remove(constructorFields.get(i-offset)).val.getAssignable();
											}
											if(offset>0) {
												args[0]=enclosingInstance;
											}
											o=DeferredConstructor.this.constructor.newInstance(args);
											if(values.size()>0) for(Map.Entry<String, Value> entry:values.entrySet()) {
												super.setField(entry.getValue().f,entry.getValue().val);
											}
											values.clear();
										} catch(Throwable t) {
											BaseException.throwException(t);
										}
									}
								} else {
									super.setField(f, v);
								}
							}

							@Override
							public not.alexa.netobjects.types.AccessibleObject getField(Field f) throws BaseException {
								if(o==null) {
									throw new BaseException(BaseException.FORBIDDEN,"Illegal access to field "+f.getName()+" of deferred object.");
								}
								return super.getField(f);
							}
						};
					}
				};
			} catch(Throwable t) {
				return BaseException.throwException(t);
			}
		}
	}
}
