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
package not.alexa.netobjects.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Stack;

import not.alexa.netobjects.Adaptable;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.TypeResolver.LoaderIntermediate;
import not.alexa.netobjects.types.access.Constructor;
import not.alexa.netobjects.types.access.Constructor.Provider;

/**
 * The default implementation of a type loader. This loader tries to resolve a type definition as follows:
 * <ol>
 * <li>If the loader has a parent, the parent is asked to resolve the type.
 * <li>The type is of type {{@link JavaClass.Type} and has a class representation with respect to the current class loader. If a static method <code>getTypeDescription</code>
 * is present, the return value of this method is the type definition.
 * <li>If additional resolvers are present, each of this resolver is called in the registered order. The first one resolving the type is taken.
 * <li>Otherwise, the method returns <code>null/code> since no type definition was found.
 * </ol>
 * The default loader can be extended using {@link TypeResolver}. Such resolvers can be registered globally for future instances of a type loader using {@link DefaultTypeLoader#addTypeResolver(TypeResolver)}
 * or using the constructor {@link DefaultTypeLoader#DefaultTypeLoader(TypeLoader, ClassLoader, List)}.
 * 
 * @author notalexa
 *
 */
public class DefaultTypeLoader extends Adaptable.Default implements TypeLoader {
    private static List<TypeResolver> defaultResolvers;
    private static final TypeDefinition NULL_TYPE=new TypeDefinition() {
        @Override
        public Flavour getFlavour() {
            return null;
        }
    };
    static final TypeLoader BASE_LOADER;
    
    private Map<Type,LinkedLocal> linkedLocals=new HashMap<>();
	private Map<ObjectType,TypeDefinition> resolved=new HashMap<>();
	protected ClassLoader loader;
	protected TypeLoader parent;
	private TypeResolver[] resolvers;
	// Used in synchronized blocks only!!
	private Intermediate intermediate=new Intermediate();
	
	/**
	 * Add a resolver to the list of default resolvers. Resolvers already contained in the list (with respect to {@link Object#equals(Object)}) are ignored.
	 * <p><b>Registration of a new resolver doesn't have any effect on type loaders already created.</b>
	 * @param resolver the resolver to add.
	 * @see TypeResolver
	 */
	public static synchronized void addTypeResolver(TypeResolver resolver) {
	    if(defaultResolvers==null) {
	        defaultResolvers=new ArrayList<TypeResolver>();
	    }
	    if(!defaultResolvers.contains(resolver)) {
	        defaultResolvers.add(resolver);
	    }
	}
	
	static {
		ServiceLoader<TypeResolver> serviceLoader=ServiceLoader.load(TypeResolver.class,DefaultTypeLoader.class.getClassLoader());
		for(TypeResolver resolver:serviceLoader) {
			addTypeResolver(resolver);
		}
	    BASE_LOADER=new DefaultTypeLoader();
	}
	
	public DefaultTypeLoader() {
	    this(null,null,defaultResolvers);
	}
	
    public DefaultTypeLoader(ClassLoader loader) {
        this(null,loader);
    }

    public DefaultTypeLoader(TypeLoader parent,ClassLoader loader) {
        this(parent,loader,defaultResolvers);
    }

	public DefaultTypeLoader(TypeLoader parent,ClassLoader loader,List<TypeResolver> resolvers) {
	    this.parent=parent;
	    this.loader=loader==null?getClass().getClassLoader():loader;
	    this.resolvers=resolvers!=null&&resolvers.size()>0?resolvers.toArray(new TypeResolver[resolvers.size()]):null;
	}
	
	@Override
    public ClassLoader getClassLoader() {
        return loader;
    }

	/**
	 * Compare with the class description for the resolution order.
	 * 
	 * @param t the type to resolve
	 * @return the resolved type definition or <code>null</code> if unknown.
	 */
    @Override
	public TypeDefinition resolveType(ObjectType t) {
		TypeDefinition type=parent==null?null:parent.resolveType(t);
		if(type==null) {
		    type=resolved.get(t);
		}
		if(type==null) {
			synchronized (this) {
				type=resolved.get(t);
				if(type==null) {
					type=TypeLoader.super.resolveType(t);
					if(type==null) {
	                    type=t.resolveDefault(this);
					}
					if(type==null&&resolvers!=null) for(TypeResolver r:resolvers) try {
						intermediate.init();
					    if((type=r.resolve(intermediate, t))!=null) {
					    	Provider provider=intermediate.providerMap.remove(t);
					    	if(provider!=null) {
					    		Constructor.addProvider(provider);
					    	}
					        break;
					    }
					} finally {
						intermediate.destruct();
					}
					if(type==null) {
						type=NULL_TYPE;
					}
					resolved.put(t, type);
				}
			}
		}
		return type==NULL_TYPE?null:type;
	}
    
    /**
     * The method caches the resolved locally linked objects since this is an expensive lookup.
     * 
     */
    @Override
    public LinkedLocal getLinkedLocal(Type type) {
        LinkedLocal linkedLocal=linkedLocals.get(type);
        if(linkedLocal==null) {
            synchronized(linkedLocals) {
                linkedLocal=linkedLocals.get(type);
                if(linkedLocal==null) {
                    linkedLocal=TypeLoader.super.getLinkedLocal(type);
                }
                if(linkedLocal==null) {
                    linkedLocal=new LinkedLocal() {
                        @Override
                        public ObjectType getType() {
                            return type;
                        }

						@Override
						public Constructor getConstructor() {
							return null;
						}
                    };
                }
                linkedLocals.put(type,linkedLocal);
            }
        }
        return linkedLocal.isLocallyLinked()?linkedLocal:null;
    }

	
	private class Intermediate implements LoaderIntermediate {
		private Map<ObjectType,TypeDefinition> map=new HashMap<>();
		private Map<ObjectType,Provider> providerMap=new HashMap<>();
		private Stack<Set<ObjectType>> registered=new Stack<>();

		@Override
		public ClassLoader getClassLoader() {
			return DefaultTypeLoader.this.getClassLoader();
		}

		@Override
		public void register(ObjectType type, TypeDefinition intermediateTypeDefinition) {
			map.put(type, intermediateTypeDefinition);
			registered.peek().add(type);
		}

		@Override
		public TypeDefinition resolveType(ObjectType type) {
			TypeDefinition def=map.get(type);
			return def==null?DefaultTypeLoader.this.resolveType(type):def;
		}
		
		private void init() {
			registered.push(new HashSet<>());
		}
		
		private void destruct() {
			for(ObjectType type:registered.pop()) {
				map.remove(type);
			}
			if(registered.size()==0) {
				map.clear();
			}
		}

		@Override
		public void addProvider(ObjectType type, Provider provider) {
			providerMap.put(type, provider);
		}
	}
}
