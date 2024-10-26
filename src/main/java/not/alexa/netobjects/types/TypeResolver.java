package not.alexa.netobjects.types;

import java.io.IOException;
import java.io.InputStream;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.access.RuntimeInfo.Provider;

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
     * @param intermediate resolver for intermediate types
     * @param type the type to resolve
     * @return the resolved type definition or <code>null/code> if not resolveable.
     */
    public TypeDefinition resolve(LoaderIntermediate intermediate,ObjectType type);
    
    /**
     * Class serving as an intermediate type resolver. All registered types are available during resolution only (if not returned by {@link TypeResolver#resolve(LoaderIntermediate, ObjectType)})
     * @author notalexa
     *
     */
	public interface LoaderIntermediate {
		
		/**
		 * 
		 * @return the class loader of this intermediate
		 */
		public ClassLoader getClassLoader();
		
		/**
		 * Register an intermediate type definition
		 * @param type the type
		 * @param intermediateTypeDefinition the intermediate type definition
		 */
		public void register(ObjectType type,TypeDefinition intermediateTypeDefinition);
		
		/**
		 * Resolve the given type taking intermediates into account.
		 * @param type the type to resolve
		 * @return the (intermediate) type definition of the type
		 */
		public TypeDefinition resolveType(ObjectType type);
		
		/**
		 * Add the provider to the system. This provider should be only registered if {@link #resolveType(ObjectType)} returns a non null value.
		 * 
		 * @type the type this provider is intended for
		 * @param provider the provider for the given type
		 */
		public void addProvider(ObjectType type,Provider provider);
	}
	
	/**
	 * Type resolver serving as a delegate. If the namespace of the type is the normal Java namespace, take the name of the type (typically the classname)
	 * and resolve the file <code>&lt;name&gt;.resolver</code>. If this file exist, the content is assumed to be a network object representing a type resolver (encoded in XML).
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
        public TypeDefinition resolve(LoaderIntermediate loader, ObjectType type) {
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
