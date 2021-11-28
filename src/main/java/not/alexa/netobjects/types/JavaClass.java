/*
 * Copyright (C) 2020 Not Alexa
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
package not.alexa.netobjects.types;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.NetworkObject;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.JavaClass.Type.InstanceSupport;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.utils.TypeUtils;
import not.alexa.netobjects.utils.TypeUtils.NameBuilder;

/**
 * This namespace represents the java class namespace. In general, a type is represented as a java class name. 
 * Additionally, the namespace introduce a version similar to the <code>serializedVersionUID</code>. This version
 * is a string and optional.
 * <br>A typical representation of such a class is
 * <pre>
 * jvm:not.alexa.example.NetObject
 * </pre>
 * for it's default version and
 * <pre>
 * jvm:not.alexa.example.NetObject:1
 * </pre>
 * or even
 * <pre>
 * jvm:not.aexa.example.NetObject:test
 * </pre>
 * for test purposes.
 * <p>Inside a JVM, the given class may have an implementation loadable by a given classloader. Taking versions into account, the underlying system may depend the class with
 * a classloader maintaing classes for the given version.
 * <br>Note that the network object may live inside a (Java-)VM even without a class representation (but represented as a DOM tree for example). In this case, the type is
 * <b>not linked to a local class</b>.  
 * 
 * 
 * 
 * @author notalexa
 *
 */
public final class JavaClass extends Namespace {
    private static ReferenceQueue<InstanceSupport> OVERLAY_RECYCLER=new ReferenceQueue<InstanceSupport>();
	private static Map<String,Class<?>> PRIMITIVE_TYPES=new HashMap<String, Class<?>>();
	static {
		PRIMITIVE_TYPES.put("boolean",Boolean.TYPE);
		PRIMITIVE_TYPES.put("char",Character.TYPE);
		PRIMITIVE_TYPES.put("byte",Byte.TYPE);
		PRIMITIVE_TYPES.put("short",Short.TYPE);
		PRIMITIVE_TYPES.put("int",Integer.TYPE);
		PRIMITIVE_TYPES.put("long",Long.TYPE);
		PRIMITIVE_TYPES.put("float",Float.TYPE);
		PRIMITIVE_TYPES.put("double",Double.TYPE);
	}
	
	private Map<String,Type> loadedTypes=new HashMap<>();
	JavaClass() {
		super(Type.class);
		register(this);
		loadedTypes.put("not.alexa.netobjects.types.JavaClass$Type",new Type(ObjectType.class.getName()));
	};
	
	/**
	 * Classes representing an object type are special and registered while creating the 
	 * namespace.
	 * 
	 * @param clazz the object type class to register (must implement {@link ObjectType})
	 */
	void registerObjectType(Class<?> clazz) {
	    if(ObjectType.class.isAssignableFrom(clazz)&&!loadedTypes.containsKey(clazz.getName())) {
	        loadedTypes.put(clazz.getName(),new Type(ObjectType.class.getName()));
	    }
	}
		
	public final class Type extends AbstractType implements ObjectType {
		
		protected String className;
		protected String method;
		protected String version;
		protected LinkedLocal preloaded;
		protected OverlayRef overlayRefs;
		
		
		/**
		 * Java classes are represented by a class name and optionally by a version
		 * 
		 * @param className the name (plus version, colon separated) of the class
		 */
		public Type(String className) {
			int p=className.indexOf(':');
			method="";
			if(p>0) {
				this.className=className.substring(0,p);
				this.version=className.substring(p+1);
				p=version.indexOf(':');
				if(p>=0) {
				    method=version.substring(p+1);
				    version=version.substring(0,p);
				}
			} else {
				this.className=className;
				this.version="";
			}
		}
		
		private void preload() {
            if(preloaded==null&&!isMethod()) try {
                preloaded=new InstanceSupport(defineClass(Type.class.getClassLoader(),this.className));
            } catch(Throwable t) {
            }
		}
		
		/**
		 * For a given class loader, this class may have a class representation somewhere in the class path.
		 * <br><i>This method doesn't do any checks concerning the version. Once a classloader is given, the class is returned if the classname
		 * of this type can be found on the class path.</i></br>
		 * 
		 * @param loader the class loader to use for loading the class
		 * @return the resolved class or <code>null</code> if the class is unknown
		 */
		public LinkedLocal asLinkedLocal(ClassLoader loader) {
			if(preloaded==null) try {
			    Class<?> clazz=defineClass(loader,className);
			    if(isMethod()) {
			      Method m=findMethod(clazz,method);
			      return new InstanceSupport(m);  
			    } else {
			        return new InstanceSupport(clazz);
			    }
			} catch(Throwable t) {
				return null;
			} else {
				return preloaded;
			}
		}
				
		public int hashCode() {
			return className.hashCode()^version.hashCode()^method.hashCode();
		}
		
		public boolean equals(Object o) {
		    if(o==this) {
		        return true;
		    }
			if(o instanceof Type) {
				Type t=(Type)o;
				return className.equals(t.className)&&version.equals(t.version)&&method.equals(t.method);
			}
			return false;
		}
		
		public String toString() {
			return getName();
		}

		public String getName() {
			if(version.length()>0) {
				return className+":"+version+(method.length()>0?":"+method:"");
			} else {
				return className+(method.length()>0?"::"+method:"");
			}
		}
		
		public String getClassName() {
			return className;
		}
		
		public String getVersion() {
			return version;
		}
		
		public String getMethod() {
		    return method;
		}
		
		public boolean isMethod() {
		    return method.length()>0;
		}
		
		/**
		 * Define the class. This is not just a class loader operation but care has to be taken for arrays.
		 * 
		 * @param loader the class loader to use
		 * @param s the class to load
		 * @return a class representing this 
		 * @throws BaseException
		 */
		protected Class<?> defineClass(ClassLoader loader,String s) throws BaseException {
			if(s.endsWith("[]")) {
				Class<?> componentClass=defineClass(loader,s.substring(0,s.length()-2));
				return Array.newInstance(componentClass,0).getClass();
			} else try {
				Class<?> clazz=PRIMITIVE_TYPES.get(s);
				return clazz==null?Class.forName(s,true,loader):clazz;
			} catch(ClassNotFoundException e) {
				throw new BaseException(BaseException.NOT_FOUND,"Class not found: "+s);
			}
		}
		
		public boolean hasOverlays() {
		    return overlayRefs!=null;
		}
		
		public InstanceSupport createInstanceSupport(ClassLoader loader,Class<?> overlay) {
		    InstanceSupport support=new InstanceSupport(overlay);
		    OverlayRef ref;
		    while((ref=(OverlayRef)OVERLAY_RECYCLER.poll())!=null) {
		        ref.release();
		    }
            if(overlay.getAnnotation(Overlay.class)!=null) {
                ref=new OverlayRef(support);
                synchronized(this) {
                    ref.next=overlayRefs;
                    if(overlayRefs!=null) {
                        overlayRefs.prev=ref;
                    }
                    overlayRefs=ref;
                }
            }
		    return support;
		}
			    
	    private Method findMethod(Class<?> clazz, String method) {
	        if(clazz.equals(Object.class)) {
	            return null;
	        }
	        for(Method m:clazz.getMethods()) {
	            Class<?>[] parameterTypes=m.getParameterTypes();
	            if(parameterTypes.length==0||!Context.class.isAssignableFrom(parameterTypes[0])) {
	                continue;
	            }
	            Class<?>[] exceptionTypes=m.getExceptionTypes();
	            boolean exceptionVerified=false;
	            for(Class<?> exceptionType:exceptionTypes) {
	                if(exceptionType.isAssignableFrom(BaseException.class)) {
	                    exceptionVerified=true;
	                    break;
	                }
	            }
	            if(!exceptionVerified) {
	                continue;
	            }
	            String id=resolveId(m);
	            if(id!=null&&id.equals(method)) {
	                return m;
	            }
	        }
	        return findMethod(clazz.getSuperclass(),method);
	    }

		
        public class InstanceSupport extends LinkedLocal {
            public InstanceSupport(Class<?> overlay) {
                super(overlay);
            }
            public InstanceSupport(Method m) {
                super(m);
            }
            @Override
            public ObjectType getType() {
                return Type.this;
            }
        }
        
        private class OverlayRef extends PhantomReference<Type.InstanceSupport> {
            private OverlayRef next;
            private OverlayRef prev;
            
            public OverlayRef(InstanceSupport referent) {
                super(referent, OVERLAY_RECYCLER);
            }
            
            private void release() {
                synchronized(Type.this) {
                    if(next!=null) {
                        next.prev=prev;
                    }
                    if(prev!=null) {
                        prev.next=next;
                    } else {
                        overlayRefs=next;
                    }
                }
            }
        }

        @Override
        public TypeDefinition resolveDefault(TypeLoader loader) {
            if(isMethod()) {
                TypeDefinition classType=loader.resolveType(create(className));
                if(classType!=null) for(MethodTypeDefinition m:classType.getMethods()) {
                    if(m.getType(getNamespace()).equals(this)) {
                        return m;
                    }
                }
            } else {
                Class<?> clazz=asLinkedLocal(loader.getClassLoader()).asClass();
                if(clazz!=null) {
                    if(clazz.isEnum()) {
                        return new EnumTypeDefinition((Class<? extends Enum<?>>)clazz);
                    } else if(clazz.isArray()) {
                        return new ArrayTypeDefinition(loader.resolveType(ObjectType.createClassType(clazz.getComponentType().getName())));
                    } else try {
                        return (TypeDefinition)clazz.getMethod("getTypeDescription").invoke(null);
                    } catch(Throwable t) {
                    }
                }
            }
            return null;
        }
	}

	/**
	 * @return the constant <code>jvm</code>
	 */
	@Override
	public String getUrnPrefix() {
		return "jvm";
	}
	
	@Override
	public Type create(String urn) {
	    return create(urn,null);
	}
	
    public Type createMethodType(Method m) {
        Type type=create(Namespace.asString(m.getDeclaringClass()),m.getDeclaringClass());
        String name=resolveId(m);
        return create(type.className+":"+type.version+":"+name,null);
    }
    
	public Type createMethodType(MethodTypeDefinition m) {
	    NameBuilder identifier=TypeUtils.createNameBuilder();
	    identifier.add(m.getName());
        for(TypeDefinition r:m.getParameterTypes()) {
            Type t=r.getJavaClassType();
            if(t==null) {
                return null;
            }
            identifier.add(t.getName());
        }
	    Type type=m.getClassType().getJavaClassType();
	    if(type==null) {
	        return null;
	    }
	    return create(type.className+":"+type.version+":"+identifier.asUUID(),null);
	}
	
	Type create(String urn,Class<?> clazz) {
        Type type=loadedTypes.get(urn);
        if(type==null) {
            String typeUrn=urn;
            if(clazz!=null) {
                Class<?> overloaded=TypeUtils.resolve(clazz);
                if(!overloaded.equals(clazz)) {
                    typeUrn=Namespace.asString(overloaded);
                    type=create(typeUrn);
                }
            }
            if(type==null) {
                type=new Type(typeUrn);
            }
            loadedTypes.put(urn,type);
            type.preload();
        }
        return type;
    }
	
    private String resolveId(Method m) {
        NetworkObject objId=TypeUtils.getNetworkObject(JavaClass.this, m);
        if(objId!=null&&"jvm".equals(objId.ns())&&!"##id".contentEquals(objId.id())) {
            return objId.id();
        }
        NameBuilder builder=TypeUtils.createNameBuilder().add(m.getName());
        Class<?>[] parameterTypes=m.getParameterTypes();
        for(int i=1;i<parameterTypes.length;i++) {
            builder.add(ObjectType.createClassType(parameterTypes[i]).getName());
        }
        return builder.asUUID().toString();
    }

}
