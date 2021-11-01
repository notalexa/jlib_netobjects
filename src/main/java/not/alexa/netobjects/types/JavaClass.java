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

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.utils.OverlayUtils;

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
		protected String version;
		protected Class<?> preloaded;
		
		
		/**
		 * Java classes are represented by a class name and optionally by a version
		 * 
		 * @param className the name (plus version, colon separated) of the class
		 */
		public Type(String className) {
			int p=className.indexOf(':');
			if(p>0) {
				this.className=className.substring(0,p);
				this.version=className.substring(p+1);
			} else {
				this.className=className;
				this.version="";
			}
			try {
				preloaded=defineClass(Type.class.getClassLoader(),this.className);
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
		public Class<?> asClass(ClassLoader loader) {
			if(preloaded==null) try {
				return defineClass(loader,className);
			} catch(Throwable t) {
				return null;
			} else {
				return preloaded;
			}
		}
				
		public int hashCode() {
			return className.hashCode()^version.hashCode();
		}
		
		public boolean equals(Object o) {
			if(o instanceof Type) {
				Type t=(Type)o;
				return version.equals(t.version)&&className.equals(t.className);
			}
			return false;
		}
		
		public String toString() {
			return getName();
		}

		public String getName() {
			if(version.length()>0) {
				return className+":"+version;
			} else {
				return className;
			}
		}
		
		public String getClassName() {
			return className;
		}
		
		public String getVersion() {
			return version;
		}
		
		/**
		 * Define the class. This is not just a class loader operation but case has to be taken for arrays.
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
	
    Type create(String urn,Class<?> clazz) {
        Type type=loadedTypes.get(urn);
        if(type==null) {
            String typeUrn=urn;
            if(clazz!=null) {
                Class<?> overloaded=OverlayUtils.resolve(clazz);
                if(!overloaded.equals(clazz)) {
                    typeUrn=Namespace.asString(overloaded);
                }
            }
            type=new Type(typeUrn);
            loadedTypes.put(urn,type);
        }
        return type;
    }
}
