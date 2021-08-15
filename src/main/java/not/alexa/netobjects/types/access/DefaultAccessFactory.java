/*
 * Copyright (C) 2021 Not Alexa
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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import not.alexa.netobjects.Adaptable;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.JavaClass;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access.SimpleTypeAccess;

/**
 * Default implementation of {@link AccessFactory}. This access is suitable for type definitions which have a local java class representation. 
 * The implementation resolves access as follows:
 * <ul>
 * <li>If the definition is primitive, an enumeration or an interface, {@link SimpleTypeAccess} is used.
 * <li>If the definition is an array, no access is returned.
 * <li>If the definition is a class type access is resolved as follows:
 * <ol>
 * <li>If the type has a java class representation, the access class is constructed using the name of the java class with <code>$ClassAccess</code>
 * appended. This class is resolved using the class loader of the referrer or context. The class should be loadable, implement {@link Access} and should contain a constructor with argument {@link AccessFactory}
 * which is called with argument this access factory.
 * <li>If additional resolvers are present, each of this resolver is called in the registered order. The first one resolving access is taken.
 * <li>Finally, an {@link AnonymousClassAccess} is created (which creates an error whenever a new instance is requested and resolves fields using
 * this factory) if the access is unknown.
 * </ol>
 * </ul>
 * 
 * 
 * As for the default type loader implementation, 
 * the default access factpry can be extended using {@link AccessResolver}. Such resolvers can be registered globally for future instances of a type loader using {@link AccessResolver#addAccessResolver(AccessFactory)}
 * or using the constructor {@link DefaultAccessFactory#DefaultTypeLoader(List)}.
 * 
 * @author notalexa
 * @see AccessResolver
 *
 */
public class DefaultAccessFactory extends Adaptable.Default implements  AccessFactory {
    private static List<AccessResolver> defaultResolvers;
    
    /**
     * Add a resolver to the list of default resolvers. Resolvers already contained in the list (with respect to {@link Object#equals(Object)}) are ignored.
     * <p><b>Registration of a new resolver doesn't have any effect on factories already created.</b>
     * @param resolver the resolver to add.
     * @see AccessResolver
     */
    public static synchronized void addAccessResolver(AccessResolver resolver) {
        if(defaultResolvers==null) {
            defaultResolvers=new ArrayList<AccessResolver>();
        }
        if(!defaultResolvers.contains(resolver)) {
            defaultResolvers.add(resolver);
        }
    }
    
    protected AccessResolver[] resolvers;

	public DefaultAccessFactory() {
	    this(defaultResolvers);
	}
	
	public DefaultAccessFactory(List<AccessResolver> resolvers) {
	    this.resolvers=resolvers==null||resolvers.size()==0?null:resolvers.toArray(new AccessResolver[resolvers.size()]);
	}

	@Override
	public final Access resolve(Context context,TypeDefinition type) {
	    return resolve(context.getTypeLoader().getClassLoader(),type);
	}
	
	@Override
	public final Access resolve(Access referrer,TypeDefinition type) {
	    return resolve(referrer.getAccessLoader(),type);
	}

	/**
	 * Useful to be called in {@link AccessResolver#resolve(DefaultAccessFactory, ClassLoader, TypeDefinition)} this method resolves access as described in the
	 * type description above.
	 * 
	 * @param usedClassLoader the class loader to use
	 * @param type the type to resolve
	 * @return the access for the type or <code>null</code> if no access is provided.
	 */
	public Access resolve(ClassLoader usedClassLoader,TypeDefinition type) {
	    ClassAccessHolder accessHolder=type.getAdapter(ClassAccessHolder.class);
	    Access access=accessHolder==null?null:accessHolder.get(usedClassLoader);
		if(access==null) try {
			switch(type.getFlavour()) {
				case InterfaceType:
				case EnumType:
				case PrimitiveType:access=new SimpleTypeAccess(type);
					break;
				case ArrayType:
					return null;
				case ClassType:
					JavaClass.Type javaType=type.getJavaClassType();
					if(javaType!=null) try {
						Class<?> accessClass=Class.forName(javaType.getName()+"$ClassAccess",true,usedClassLoader);
						Constructor<?> c=accessClass.getConstructor(AccessFactory.class);
						if(Access.class.isAssignableFrom(accessClass)) {
							access=(Access)c.newInstance(this);
						}
					} catch(Throwable t) {
					}
					if(access==null&&resolvers!=null) for(AccessResolver resolver:resolvers) {
					    if((access=resolver.resolve(this, usedClassLoader, type))!=null) {
					        break;
					    }
					}
					if(access==null) {
						access=new AnonymousClassAccess(usedClassLoader,type);
					}
					break;
			}
			if(accessHolder==null) {
			    type.putAdapter(ClassAccessHolder.class,new ClassAccessHolder(usedClassLoader,access));
			} else if(usedClassLoader!=null) {
			    accessHolder.put(usedClassLoader, access);
			}
		} catch(Throwable t) {
		}
		return access;
	}
	
	private class AnonymousClassAccess implements Access {
		private TypeDefinition type;
		private ClassLoader loader;
		private AnonymousClassAccess(ClassLoader loader,TypeDefinition type) {
			this.type=type;
			this.loader=loader;
		}
		
		@Override
        public ClassLoader getAccessLoader() {
            return loader;
        }

        @Override
		public TypeDefinition getType() {
			return type;
		}

		@Override
		public AccessibleObject newInstance(AccessContext context) throws BaseException {
			throw new BaseException(BaseException.BAD_REQUEST,"No instance of anonymous type "+type.toString());
		}
		
		@Override
		public AccessibleObject makeInstance(Object v) throws BaseException {
			return new DefaultAccessibleObject(this, v);
		}

		@Override
		public Access getFieldAccess(Field f) throws BaseException {
			return resolve(this,f.getType());
		}	
	}
	
	private class ClassAccessHolder {
	    Access defaultAccess;
	    Map<ClassLoader,Access> loaderAccess;
	    private ClassAccessHolder(ClassLoader loader,Access access) {
	        if(loader!=null&&loader!=ClassAccessHolder.class.getClassLoader()) {
    	        loaderAccess=new HashMap<ClassLoader, Access>();
    	        loaderAccess.put(loader,access);
	        } else {
	            defaultAccess=access;
	        }
	    }
	    
	    private Access put(ClassLoader loader,Access access) {
	        if(loaderAccess==null) {
	        } else {
	            loaderAccess.put(loader,access);
	            return access;
	        }
            return access;
	    }
	    
	    public Access get(ClassLoader loader) {
	        return loaderAccess==null?defaultAccess:loaderAccess.get(loader);
	    }
	}
	
	public interface AccessResolver {
	    /**
	     * Resolve access . The implementation should
	     * take care of infinite loops while looking up access using the supplied factpry.
	     * 
	     * @param factory the default access factory requesting the access
	     * @param type the type we need access for
	     * @return the resolved type definition or <code>null/code> if not resolveable.
	     */
	    public Access resolve(DefaultAccessFactory factory,ClassLoader currentClassLoader,TypeDefinition type);
	}
}
