/*
 * Copyright (C) 2024 Not Alexa
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
package not.alexa.netobjects.coding;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.AccessFactory;

/**
 * Basic class for coding schemes. The class provides
 * <ul>
 * <li>The root type of the scheme
 * <li>The access factory of the scheme
 * <li>The namespace of the scheme
 * <li>Mime type and file extension of the scheme
 * </ul>
 */
public abstract class AbstractCodingScheme implements CodingScheme, Cloneable {
    protected AccessFactory factory;
    TypeDefinition rootType;
    protected Namespace namespace=Namespace.getJavaNamespace();
    protected String mimeType;
    protected String fileExtension;

	protected AbstractCodingScheme(AccessFactory factory) {
		this.factory=factory;
	}
	
    public final AccessFactory getFactory() {
        return factory;
    }

    public final TypeDefinition getRootType(Context context,Class<?> clazz) {
    	if(rootType==null) {
    		return context.resolveType(clazz);
    	} else {
    		return rootType;
    	}
    }
    
    
    public final Namespace getNamespace() {
        return namespace;
    }

    public final String getMimeType() {
        return mimeType;
    }
    
    public final String getFileExtension() {
        return fileExtension;
    }

    /**
     * Generic abstract builder class which can be extended by builders for non abstract coding schemes.
     * 
     * @author notalexa
     *
     * @param <T> the coding scheme class this builder creates
     * @param <B> the builder class itself
     */
    public static abstract class Builder<T extends AbstractCodingScheme,B extends Builder<T,B>> {
        protected T scheme;

        public Builder(T scheme) {
        	this.scheme=cloneScheme(scheme);
        }        
        
        @SuppressWarnings("unchecked")
        protected T cloneScheme(T scheme) {
            try {
                return (T)scheme.clone();
            } catch(Throwable t) {
                return null;
            }
        }

        /**
         * Typically mime type and file extension are set from the coding scheme
         * to a suitable value.
         * 
         * @param mimeType the mime type of data the scheme can handle
         * @param fileExtension the typical file extension for data the scheme can handle
         * @return this for additional configuration
         */
        public B setTypeInfos(String mimeType,String fileExtension) {
            scheme.mimeType=mimeType;
            scheme.fileExtension=fileExtension;
            return myself();
        }

        /**
         * 
         * @param ns the namespace of the new coding scheme
         * @return this for additional configuration
         */
        public B setNamespace(Namespace ns) {
            scheme.namespace=ns;
            return myself();
        }

        /**
         * 
         * @param rootClass the root class of the new coding scheme
         * @return this for additional configuration
         */
        public B setRootType(Class<?> rootClass) {
            scheme.rootType=new DefaultTypeLoader(rootClass.getClassLoader()).resolveType(rootClass);
            return myself();
        }
        
        /**
         * 
         * @param rootType the root type of the new coding scheme
         * @return this for additional configuration
         */
        public B setRootType(TypeDefinition rootType) {
            scheme.rootType=rootType;
            return myself();
        }
        
        /**
         * Set the access factory for the new coding scheme.
         * 
         * @param factory the access factory of the new coding scheme
         * @return this for additional configuration
         */
        public B setAccessFctory(AccessFactory factory) {
            scheme.factory=factory;
            return myself();
        }
        
        protected T build(boolean initCodecs) {
            return cloneScheme(scheme);
        }
        
        /**
         * 
         * @return the new coding scheme
         */
        public T build() {
            return build(false);
        }
        
        /**
         * Implement this to return <code>this</code>.
         * 
         * @return myself
         */
        @SuppressWarnings("unchecked")
		protected final B myself() {
        	return (B)this;
        }
    }
}
