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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.NetworkObject;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.JavaClass.Type.InstanceSupport;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.access.RuntimeInfo;
import not.alexa.netobjects.utils.TypeUtils;
import not.alexa.netobjects.utils.TypeUtils.NameBuilder;
import not.alexa.netobjects.utils.TypeUtils.ResolvedClass;

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
	private static final Type[] NO_PARAMETER_TYPES=new Type[0];
	static final LinkedLocal[] NO_PARAMETER_CLASSES=new LinkedLocal[0];
	
    private static ReferenceQueue<InstanceSupport> OVERLAY_RECYCLER=new ReferenceQueue<InstanceSupport>();
	private static Map<String,Class<?>> PRIMITIVE_TYPES=new HashMap<String, Class<?>>();
	private static Map<String,Class<?>> INSTANCE_TYPE=new HashMap<String, Class<?>>();
	static {
		PRIMITIVE_TYPES.put("boolean",Boolean.TYPE);
		PRIMITIVE_TYPES.put("char",Character.TYPE);
		PRIMITIVE_TYPES.put("byte",Byte.TYPE);
		PRIMITIVE_TYPES.put("short",Short.TYPE);
		PRIMITIVE_TYPES.put("int",Integer.TYPE);
		PRIMITIVE_TYPES.put("long",Long.TYPE);
		PRIMITIVE_TYPES.put("float",Float.TYPE);
		PRIMITIVE_TYPES.put("double",Double.TYPE);
		
		INSTANCE_TYPE.put("boolean",Boolean.class);
		INSTANCE_TYPE.put("char",Character.class);
		INSTANCE_TYPE.put("byte",Byte.class);
		INSTANCE_TYPE.put("short",Short.class);
		INSTANCE_TYPE.put("int",Integer.class);
		INSTANCE_TYPE.put("long",Long.class);
		INSTANCE_TYPE.put("float",Float.class);
		INSTANCE_TYPE.put("double",Double.class);
	}
	
	private Map<String,Type> loadedTypes=new HashMap<>();
	JavaClass() {
		super(Type.class);
		register(this);
		loadedTypes.put("not.alexa.netobjects.types.JavaClass$Type",new Type(ObjectType.class.getName()));
		loadedTypes.put(Boolean.class.getName(),create(Boolean.TYPE.getName()));
        loadedTypes.put(Character.class.getName(),create(Character.TYPE.getName()));
        loadedTypes.put(Byte.class.getName(),create(Byte.TYPE.getName()));
        loadedTypes.put(Short.class.getName(),create(Short.TYPE.getName()));
        loadedTypes.put(Integer.class.getName(),create(Integer.TYPE.getName()));
        loadedTypes.put(Long.class.getName(),create(Long.TYPE.getName()));
        loadedTypes.put(Float.class.getName(),create(Float.TYPE.getName()));
        loadedTypes.put(Double.class.getName(),create(Double.TYPE.getName()));
	};
	
	void loadResources() {
		try {
			for(Enumeration<URL> e=JavaClass.class.getClassLoader().getResources("META-INF/typemappers");e.hasMoreElements();) try(BufferedReader reader=new BufferedReader(new InputStreamReader(e.nextElement().openStream()))) {
				String line;
				while((line=reader.readLine())!=null) try {
					JavaClassTypeMapper<?,?> mapper=((JavaClassTypeMapper<?,?>)Class.forName(line.trim(),false,JavaClass.class.getClassLoader()).newInstance());
					add(mapper);
				} catch(Throwable t) {
				}
			}
		} catch(Throwable t) {
		}
	}
	
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
	
	private void add(JavaClassTypeMapper<?,?> mapper) {
		loadedTypes.put(mapper.getSourceClass().getName(), ObjectType.createClassType(mapper.getCodingClass()));
		mapper.install();
	}
		
	public final class Type extends Namespace.AbstractType implements ObjectType {
		
		protected String className;
		protected Type[] parameterTypes;
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
			this(className,NO_PARAMETER_TYPES);
		}
		
		private Type(String className,Type[] parameterTypes) {
			this.parameterTypes=parameterTypes;
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
		
		private Type(String className,String version) {
			this.parameterTypes=NO_PARAMETER_TYPES;
			this.className=className;
			this.version=version;
			this.method="";
		}

		
		private void preload() {
            if(preloaded==null&&!isMethod()&&parameterTypes.length==0) try {
            	Class<?> instanceClass=INSTANCE_TYPE.get(className);
                preloaded=new InstanceSupport(instanceClass==null?defineClass(Type.class.getClassLoader(),this.className):instanceClass,NO_PARAMETER_CLASSES);
            } catch(Throwable t) {
            }
		}
		
		public boolean hasParameters() {
			return parameterTypes.length>0;
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
			    } else if(parameterTypes.length==0) {
			        return new InstanceSupport(clazz,NO_PARAMETER_CLASSES);
			    } else {
			    	LinkedLocal[] linkedParameters=new LinkedLocal[parameterTypes.length];
			    	for(int i=0;i<linkedParameters.length;i++) {
			    		linkedParameters[i]=parameterTypes[i].asLinkedLocal(loader);
			    	}
			    	return new InstanceSupport(clazz, linkedParameters);
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
				if(className.equals(t.className)&&version.equals(t.version)&&method.equals(t.method)&&t.parameterTypes.length==parameterTypes.length) {
					if(parameterTypes.length>0) {
						for(int i=0;i<parameterTypes.length;i++) {
							if(!parameterTypes[i].equals(t.parameterTypes[i])) {
								return false;
							}
						}
					}
					return true;
				}
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
		
		public Type getArrayType() {
			if(!isMethod()) {
				if(preloaded!=null) try {
					Class<?> arrayClass=defineClass(Type.class.getClassLoader(), className+"[]");
					return ObjectType.createClassType(arrayClass);
				} catch(Throwable t) {
				}
				return new Type(className+"[]",version);
			}
			return null;
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
		    InstanceSupport support=new InstanceSupport(asLinkedLocal(loader),overlay);
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
        	LinkedLocal clazz;
        	LinkedLocal[] parameters;
        	/**
        	 * Constructor for network objects
        	 * @param clazz
        	 */
            public InstanceSupport(Class<?> clazz,LinkedLocal[] parameters) {
            	super(clazz);
            	this.clazz=this;
            	this.parameters=parameters;
            }
            
            /**
             * Constructor for overlays
             * 
             * @param clazz the underlying base class
             * @param overlay the overlay class
             */
            InstanceSupport(LinkedLocal clazz,Class<?> overlay) {
                super(overlay);
                this.clazz=clazz;
                // Not used
                this.parameters=NO_PARAMETER_CLASSES;
            }
            public InstanceSupport(Method m) {
                super(m);
            }
            
            
            @Override
            public ObjectType getType() {
                return Type.this;
            }
			@Override
			public RuntimeInfo getRuntimeInfo() {
				return isClass()?RuntimeInfo.get(clazz,this):null;
			}

			@Override
			public ResolvedClass asResolvedClass() {
				return clazz==this?resolveInternal():clazz.asResolvedClass();
			}
			
			private ResolvedClass resolveInternal() {
				Class<?> instanceClass=asClass();
				if(instanceClass!=null) {
					if(parameterTypes.length>0) {
						ResolvedClass[] parameters=new ResolvedClass[this.parameters.length];
						for(int i=0;i<parameters.length;i++) {
							parameters[i]=this.parameters[i].asResolvedClass();
						}
						return new ResolvedClass(instanceClass, parameters);
					} else {
						return TypeUtils.resolveClass(instanceClass);
					}
				} else {
					return null;
				}
			}

			@Override
			public LinkedLocal[] getParameters() {
				return clazz==this?parameters:clazz.getParameters();
			}

			@Override
			public boolean hasParameters() {
				return clazz==this?parameters.length>0:clazz.hasParameters();
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
            	LinkedLocal linkedLocal=asLinkedLocal(loader.getClassLoader());
                Class<?> clazz=linkedLocal==null?null:linkedLocal.asClass();
                if(clazz!=null) {
                    if(clazz.isEnum()) {
                        return new EnumTypeDefinition((Class<? extends Enum<?>>)clazz);
                    } else if(clazz.isArray()) {
                        return new ArrayTypeDefinition(loader.resolveType(ObjectType.createClassType(clazz.getComponentType().getName())));
                    } else if(clazz.isInterface()||Modifier.isAbstract(clazz.getModifiers())) {
                    	return new InterfaceTypeDefinition(clazz);
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
	
	
	public Type create(ResolvedClass clazz) {
		if(clazz.getParameters().length==0) {
			return create(Namespace.asString(clazz.getResolvedClass()));
		} else {
			Type[] parameterTypes=new Type[clazz.getParameters().length];
			for(int i=0;i<parameterTypes.length;i++) {
				parameterTypes[i]=create(clazz.getParameters()[i]);
			}
			return new Type(Namespace.asString(clazz.getResolvedClass()),parameterTypes);
		}		
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
