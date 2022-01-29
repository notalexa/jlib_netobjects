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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import not.alexa.netobjects.Adaptable;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.JavaClass.Type;

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
    
    private Map<Type,LinkedLocal> linkedLocals=new HashMap<>();
	private Map<ObjectType,TypeDefinition> resolved=new HashMap<>();
	protected ClassLoader loader;
	protected TypeLoader parent;
	private TypeResolver[] resolvers;
	
	/**
	 * Add a resolver to the list of default resolvers. Resolvers already contained in the list (with respect to {@link Object#equals(Object)}) are ignored.
	 * <p><b>Registration of a new resolver doesn't have any effect on type loaders already created.</b>
	 * @param resolver the resolver to add.
	 * @see TypeResolver
	 */
	public static synchronized void addTypeResolver(TypeResolver resolver) {
	    if(defaultResolvers==null) {
	        defaultResolvers=new ArrayList<DefaultTypeLoader.TypeResolver>();
	    }
	    if(!defaultResolvers.contains(resolver)) {
	        defaultResolvers.add(resolver);
	    }
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
					if(type==null&&resolvers!=null) for(TypeResolver r:resolvers) {
					    if((type=r.resolve(this, t))!=null) {
					        break;
					    }
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
                    };
                }
                linkedLocals.put(type,linkedLocal);
            }
        }
        return linkedLocal.isLocallyLinked()?linkedLocal:null;
    }



    /**
     * Interface for type resolution extensions. Typical examples are resolvers for namespaces different from the normal java namespace or
     * resolvers using annotation schemes like <a href="https://www.oracle.com/technical-resources/articles/javase/jaxb.html">JAXB</a>.
     * 
     * @author notalexa
     *
     */
	public interface TypeResolver {
	    /**
	     * Resolve the given type. Neither the parent nor the default lookup resolved the type. The implementation should
	     * take care of infinite loops while looking up a type using the supplied type loader.
	     * 
	     * @param loader the default type loader requesting the type
	     * @param type the type
	     * @return the resolved type definition or <code>null/code> if not resolveable.
	     */
	    public TypeDefinition resolve(DefaultTypeLoader loader,ObjectType type);
	}
	
	/**
	 * Type resolver serving as a delegate. If the namespace of the type is the normal Java namespace, take the name of the type (typically the classname)
	 * and resolve the file <code>&lt;name&gt;.resolver</code>. If this file exist, the content is assumed to be a network object representing a type resolver.
	 * This resolver is instantiated and the type is resolved using the this resolver.
	 * 
	 * @author notalexa
	 *
	 */
	public static class TypeResolverDelegate implements TypeResolver {
	    private Context context;
	    
	    /**
	     * Construct the delegate
	     * 
	     * @param context the context to resolve the type resolver for a given class.
	     */
	    public TypeResolverDelegate(Context context) {
	        this.context=context;
	    }

        @Override
        public TypeDefinition resolve(DefaultTypeLoader loader, ObjectType type) {
            if(type.getNamespace()==Namespace.getJavaNamespace()) {
                String n=type.getName().replace('.','/')+".resolver";
                try(InputStream stream=loader.getClassLoader().getResourceAsStream(n)) {
                    if(stream!=null) {
                        TypeResolver resolver=XMLCodingScheme.DEFAULT_SCHEME.createDecoder(context, stream).decode(TypeResolver.class);
                        if(resolver!=null) {
                            return resolver.resolve(loader, type);
                        }
                    }
                } catch(IOException|BaseException e) {
                    // Silently ignore problems in this case
                }
            }
            return null;
        }
	}
}
