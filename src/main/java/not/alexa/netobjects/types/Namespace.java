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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import not.alexa.netobjects.Adaptable;

/**
 * Typically, an object "lives" in it's own execution environment. JVM classes inside a Java Virtual Machine, .NET classes inside a .NET environment and so on. For network
 * objects, this is not longer true. They are defined in a cross platform manner. This implies, that identification of types cannot be defined choosing just a special
 * naming convention. Different conventions need to be considered defined by
 * <ul>
 * <li>The platform (Java, .NET,...)
 * <li>The coding scheme used (Object Identifiers in ASN.1,...)
 * </ul>
 * for example.
 * For this purposes, the library defines the notation of a namespace. Different namespaces represents just different naming conventions <b>for the same type </b>. A type
 * definition in this framework may have different types but <b>a unique type per namespae</b>. Obviously, inside a specific platform, one namespace representing the
 * platform conventions is of special interest. Inside a Java VM (and therefore for this library), the special namespace is {@link JavaClass} representing the java class
 * naming conventions.
 * <p>Even inside a platform, versionizing a type is a problem. In most cases, this is done by versionizing a bigger artifact like the libary itself and introduce a
 * deployment or build mechanism to choose the right one. In the case of network objects, this is obviously even more problematic. Nevertheless, no explicit version mechanism
 * was introduced into the namespace concept. The reasons are simplicity and a lack of universality. In most cases, different versions can be handled as in the case of just one
 * platform. In this cases, an explicit versionizing concept introduce a complexity which has no real benefit. And since the version has to be reflected by all types, most or
 * all of the common coding mechanism cannot be used.
 * <br>Similar to the normal <code>Serializable</code> mechanism using a <code>serializedVersionUID</code> to indicate a change of version, the <code>JavaClass</code> namespace introduce
 * an (optional) version which can be evaluated if needed.
 * <p>
 * @author notalexa
 * 
 * @see JavaClass
 * @see ObjectType
 */
public abstract class Namespace {
    private static final JavaClass JAVA_CLASS_INSTANCE;
	private static Namespace[] namespaces=new Namespace[0];
	private static Map<String,Namespace> URN_MAP=new HashMap<>();
	private static Set<Class<?>> TYPE_MAP=new HashSet<>();
	int ordinal=-1;
	Class<? extends ObjectType> typeClass;
	static {
		JAVA_CLASS_INSTANCE=new JavaClass();
	}
	
	protected Namespace(Class<? extends ObjectType> typeClass) {
		this.typeClass=typeClass;
		if(namespaces.length>0&&!register(this)) {
			throw new IllegalStateException("Already registered: Namespace "+getUrnPrefix());
		}
	}
	
	static Namespace getJavaNamespace() {
		return namespaces[0];
	}
		
	synchronized static boolean register(Namespace ns) {
		if(ns.ordinal>=0||URN_MAP.containsKey(ns.getUrnPrefix())) {
			return false;
		}
		ns.ordinal=namespaces.length;
		namespaces=Arrays.copyOf(namespaces,namespaces.length+1);
		namespaces[ns.ordinal]=ns;
		URN_MAP.put(ns.getUrnPrefix(),ns);
		TYPE_MAP.add(ns.typeClass);
		if(JAVA_CLASS_INSTANCE!=null) {
		    JAVA_CLASS_INSTANCE.registerObjectType(ns.typeClass);
		}
		return true;
	}
	
	/**
	 * Main method to get an object type out of a text representation. The representation is of the format <code>&lt;namespace&gt;:&lt;type&gt;</code> where the type is
	 * namespace specific.
	 * <br>Obviously, a type definition can be represented by namespaces, which are unknown to the system. In this case, the type is definitely not linked to a local
	 * java class, but generic operations can be performed. If a new namespace is recognized, an instance of {@link UnknownNamespace} is used to generate
	 * a specific representation for this namespace.
	 * 
	 * 
	 * @param urn the type to resolve
	 * @return an object type representing the type.
	 * @throws IllegalArgumentException if the namespace cannot be resolved because no colon is found inside the string
	 */
	static ObjectType resolve(String urn) {
		int p=urn.indexOf(':');
		if(p>0) {
			Namespace ns=URN_MAP.get(urn.substring(0,p));
			if(ns==null) {
				synchronized (Namespace.class) {
					ns=URN_MAP.get(urn.substring(0,p));
					if(ns==null) {
						String prefix=urn.substring(0, p);
						ns=new UnknownNamespace() {
							@Override
							public String getUrnPrefix() {
								return prefix;
							}
							
						};
					}
				}
			}
			if(ns!=null) {
				return ns.create(urn.substring(p+1));
			}
		}
		throw new IllegalArgumentException("missing colon in type "+urn);
	}
	
	/**
	 * 
	 * @return the prefix of this namespace identifying it uniquely
	 */
	public abstract String getUrnPrefix();
	
	/**
	 * Create a type for the given urn. The namespace component is already stripped from the urn.
	 * 
	 * @param urn the urn of the type
	 * @return the resolved type
	 */
	public abstract ObjectType create(String urn);
	
	/**
	 * Base class for types
	 * 
	 * @author notalexa
	 *
	 */
	public abstract class AbstractType implements ObjectType {
		
		/**
		 * <qreturn the defining namespace
		 */
		public final Namespace getNamespace() {
			return Namespace.this;
		}
		
		/**
		 * 
		 * @return the name (the type part of the urn) of this type
		 */
		public abstract String getName();
		
		/**
		 * 
		 */
		public final String getUrn() {
			return getUrnPrefix()+":"+getName();
		}

	}
	
	/**
	 * Base class for type definitions. As mentioned, type definitions may have more than one unique type. This class organizes the types per namespace.
	 * <br>This class implements the {@link Iterable} interface iterating over all registered types
	 * 
	 * @author notalexa
	 *
	 */
	public static class Types extends Adaptable.Default implements Iterable<ObjectType> {
		protected ObjectType[] types=new ObjectType[namespaces.length];
		// First index in types which is not null
		protected int firstIndex=Integer.MAX_VALUE;
		/**
		 * 
		 * @return the java class type. This is obviously a very important type on this platform.
		 * 
		 */
		public JavaClass.Type getJavaClassType() {
			return (JavaClass.Type)types[0];
		}
		
		/**
		 * 
		 * @param ns the namespace
		 * @return the type in this namespace or <code>null</code> if the type is unknown in the given namespace
		 */
		public ObjectType getType(Namespace ns) {
			int o=ns.ordinal;
			if(o<0) {
				throw new ArrayIndexOutOfBoundsException(o);
			}
			if(o>=types.length) {
				types=Arrays.copyOf(types,o+1);
			}
			return types[o];
		}
		
		/**
		 * Add the additional types. If already set, the new type is ignored.
		 * 
		 * @param objectTypes the additional object types
		 */
		public void addTypes(ObjectType...objectTypes) {
			if(objectTypes!=null&&objectTypes.length>0) for(ObjectType type:objectTypes) {
				ObjectType old=getType(type.getNamespace());
				if(old==null) {
					firstIndex=Math.min(firstIndex, type.getNamespace().ordinal);
					types[type.getNamespace().ordinal]=type;
				}
			}
		}
		
		/**
		 * The order of types in the returned list is somewhat undefined. The only convention is that the normal java class type is at position
		 * 0 (if defined).
		 * 
		 * @return a list of all currently defined object types
		 */
		public List<ObjectType> getTypes() {
			List<ObjectType> types=new ArrayList<ObjectType>();
			for(ObjectType type:this.types) {
				if(type!=null) {
					types.add(type);
				}
			}
			return types;
		}
		
		/**
		 * Intimately related to {@link #getJavaClassType()}, this method returns the locally linked java class with respect to the given
		 * class loader (if any).
		 * 
		 * @param classLoader the class loader to use
		 * @return the java class linked to this types if any.
		 */
		public Class<?> asClass(ClassLoader classLoader) {
			if(types[0]!=null) {
				((JavaClass.Type)types[0]).asClass(classLoader);
			}
			return null;
		}
		
		public String toString() {
			return types[0]!=null?types[0].toString():"<anonymous>";
		}
		
		public boolean isAnonymous() {
			return firstIndex==Integer.MAX_VALUE;
		}

		/**
		 * Check consistency between different types. Types are consistent if
		 * all intersecting object type equals (same type) or all intersecting object types are not equal (not same type).
		 * Types are inconsistent, if they are not consistent (that is they have at least two intersecting types and one is equals and one is not
		 * equal).
		 * 
		 * @param other the type to check agains
		 * @return <ul>
		 * <li>-1 if types are inconsistent
		 * <li>0 if types are consistent and equals
		 * <li>1 if types are consistent and differ
		 * </ul>
		 */
		public int check(Types other) {
			int ret=0;
			for(ObjectType t:types) {
				if(t!=null) {
					ObjectType o=other.getType(t.getNamespace());
					if(o!=null) {
						if(o.equals(t)) {
							if(ret==-1) {
								// inconsistent
								return -1;
							}
							ret=1;
						} else {
							if(ret==1) {
								// inconsistent
								return -1;
							}
							ret=-1;
						}
					}
				}
			}
			switch(ret) {
				case 1:return 0;
				case 0:
				case -1:return 1;
			}
			return ret==1?0:1;
		}

		@Override
		public Iterator<ObjectType> iterator() {
			if(firstIndex>=types.length) {
				return Collections.emptyIterator();
			} else {
				return new Iterator<ObjectType>() {
					int index=firstIndex;
					@Override
					public boolean hasNext() {
						return index<types.length;
					}

					@Override
					public ObjectType next() {
						ObjectType type=types[index++];
						while(index<types.length) {
							if(types[index]!=null) {
								break;
							}
						}
						return type;
					}
					
				};
			}
		}
	}
	
	/**
	 * Internal abstract namespace representing an undefined namespace inside this VM.
	 * 
	 * @author notalexa
	 *
	 */
	private static abstract class UnknownNamespace extends Namespace {
		private UnknownNamespace() {
			super(Type.class);
		}
		
		@Override
		public ObjectType create(String urn) {
			return new Type(urn);
		}
		
		private class Type extends AbstractType {
			String urn;
			private Type(String urn) {
				this.urn=urn;
			}
			@Override
			public String getName() {
				return urn;
			}
			
			public String toString() {
				return getUrn();
			}
			
			public int hashCode() {
				return ordinal^urn.hashCode();
			}
			
			public boolean equals(Object other) {
				if(other instanceof Type) {
					Type t=(Type)other;
					return t.getNamespace().ordinal==getNamespace().ordinal&&t.urn.equals(urn);
				}
				return false;
			}
		}
	}

	static String asString(Class<?> clazz) {
		if(clazz.isArray()) {
			return asString(clazz.getComponentType())+"[]";
		} else {
			return clazz.getName();
		}
	}
}
