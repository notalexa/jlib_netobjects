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
package not.alexa.netobjects.coding;

import java.nio.charset.Charset;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.text.EnumCodec;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessFactory;

/**
 * Base class for text coding schemes. This class defines common attributes useful for all schemes
 * which can be configured using a builder. If a coding scheme is given and some parameters needs to be
 * modified, a builder based on the current scheme can be created using a <code>newBuilder</code> method defined
 * in a derived class and modify the attributes in the given builder. Attributes provided by this base class are:
 * <ul>
 * <li><i>Indentation:</i> Indentation can be configured using the {@link #getLineTerminator()} for line breaking (usually a <code>#0x0a</code> or <code>#0x0a#0x0d</code>)
 * and [@link {@link #getIndent()} for indentation. This parameters can be set using {@link Builder#setIndent(String, String)}.
 * <li><i>Type infos:</i> The (recommended) file extension and mime type of data produced by this codingscheme can be set using {@link Builder#setTypeInfos(String, String)} and
 * obtained using {@link #getFileExtension()} and {@link #getMimeType()}.
 * <li><i>Charecter Set:</i> The character set used by this coding scheme can be configured using {@link Builder#setCharset(Charset)} or {@link Builder#setCharset(String)} and 
 * retrieved using {@link #getCodingCharset().
 * <li><i>Root type:</i> The root type of this coding scheme. This type is typically either a concrete class type (or primitive type) or an interface indicating that the
 * scheme understands a set of types classified by the interface type. The default root type is the primitive type "object" and can be configured using {@link Builder#setRootType(TypeDefinition)}
 * and retrieved using {@link #getRootType()}.
 * <li><i>Special codecs:</i> The scheme defines an initial set of codecs (for the primitive types). Special codecs can be added using the {@link Builder#addCodec(ObjectType, Codec)} method.
 * Typical examples are byte arrays or numbers, which should be hex encoded.
 * <li><i>Namespaces:</i> The namespace used by the coding scheme can be configured. By default, the namespace is the Java Namespace with type attribute <code>class</code> but this can be configured using {@link Builder#setNamespace(String, Namespace)}.
 * <li><i>Access Factories:</i> The scheme needs specific access to types for object creation and setting and getting attributes. This {@link Access} can be retrieved using the {@link AccessFactory}
 * which can be set using {@link Builder#setAccessFctory(AccessFactory)}.
 * <li><i>Reserved attributes:</i> Most schemes uses reserved attributes indicating special internal values. For example, if the scheme supports object references as needed if {@link ClassTypeDefinition#enableObjectRefs()}
 * is <code>true</code>, the recommended attributes or fields in the encoded text should be <code>obj-ref</code> for the reference and <code>obj-id</code> for the id of the object. To be
 * flexible, this attributes can be overridden using {@link Builder#setReservedAttributes(ReservedAttributes)} and retrieved using {@link #getReservedAttributes()}.
 * </ul>
 * @author notalexa
 *
 */
public abstract class AbstractTextCodingScheme implements CodingScheme, Cloneable {
    protected Charset charset;
    protected AccessFactory factory;
    protected TypeDefinition rootType;
    protected String indent="";
    protected String lineTerminator="";
    protected String mimeType;
    protected String fileExtension;
    protected Namespace namespace=Namespace.getJavaNamespace();
    protected String typeRef="class";
    protected ReservedAttributes reservedAttributes=new ReservedAttributes("obj-ref","obj-id");
    protected Codecs initialCodecs;
    protected Codecs codecs;
    protected AbstractTextCodingScheme(Charset charset,AccessFactory factory) {
        this.charset=charset;
        this.factory=factory;
        rootType=PrimitiveTypeDefinition.getTypeDescription(Object.class);
        initialCodecs=Codecs.defaultTextCodecs();
        codecs=initialCodecs.copy();
    }
    
    @Override
    public Charset getCodingCharset() {
        return charset;
    }
    
    public AccessFactory getFactory() {
        return factory;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public String getFileExtension() {
        return fileExtension;
    }

    public TypeDefinition getRootType() {
        return rootType;
    }
    
    public String getIndent() {
        return indent;
    }
    
    public String getLineTerminator() {
        return lineTerminator;
    }
    
    public ReservedAttributes getReservedAttributes() {
        return reservedAttributes;
    }
    
    public Namespace getNamespace() {
        return namespace;
    }
    
    public String getTypeRef() {
        return typeRef;
    }
    
    public Codec getCodec(Context context,ObjectType type,Access access) throws BaseException {
        if(access!=null) {
            Object key=access.getAccessKey(type);
            Codec codec=codecs.get(key);
            if(codec==null) {
                for(ObjectType ot:access.getType().getTypes()) {
                    codec=codecs.get(access.getAccessKey(ot));
                    if(codec!=null) {
                        codecs.put(key, codec);
                        break;
                    }
                }
            }
            if(codec==null) {
                switch(access.getType().getFlavour()) {
                    case PrimitiveType:throw new BaseException(BaseException.NOT_FOUND,"Primitive type "+access.getType()+" has no predefined codec.");
                    case InterfaceType:codec=createInterfaceCodec(context, type, access.getType());
                        break;
                    case ArrayType:codec=createArrayCodec(context, type, access.getType());
                        break;
                    case MethodType:codec=createMethodCodec(context, type, access.getType());
                        break;
                    case EnumType:codec=createEnumCodec(context, type, access.getType());
                        break;
                    case ClassType:codec=createClassCodec(context,type,access);
                        break;
                    case UnknownType:return null;
                }
                if(codec!=null) {
                    codecs.put(key, codec);
                }
            }
            return codec;
        }
        return null;
    }

    protected Codec createInterfaceCodec(Context context,ObjectType type,TypeDefinition typeDef) throws BaseException {
        throw new BaseException(BaseException.NOT_FOUND,"Codecs for interface types are not defined.");
    }
    
    protected Codec createMethodCodec(Context context,ObjectType type,TypeDefinition typeDef) throws BaseException {
        throw new BaseException(BaseException.NOT_FOUND,"Codecs for method types are not defined.");
    }

    protected Codec createArrayCodec(Context context,ObjectType type,TypeDefinition typeDef) throws BaseException {
        throw new BaseException(BaseException.NOT_FOUND,"Codecs for array types are not defined.");
    }
    
    protected Codec createEnumCodec(Context context,ObjectType type,TypeDefinition typeDef) throws BaseException {
        return typeDef.getJavaClassType()==null?null:new EnumCodec(typeDef.getJavaClassType());
    }
    
    protected Codec createClassCodec(Context context,ObjectType type,Access access) throws BaseException {
        return null;
    }
    
    /**
     * Generic abstract builder class which can be extended by builders for non abstract coding schemes.
     * 
     * @author notalexa
     *
     * @param <T> the coding scheme class this builder creates
     * @param <B> the builder class itself
     */
    public static abstract class Builder<T extends AbstractTextCodingScheme,B extends Builder<T,B>> {
        protected T scheme;
        private Codecs savedInitialCodecs;
        
        public Builder(T scheme) {
            try {
                savedInitialCodecs=scheme.initialCodecs;
                this.scheme=cloneScheme(scheme);
            } catch(Throwable t) {
            }
        }
        
        @SuppressWarnings("unchecked")
        public T cloneScheme(T scheme) {
            try {
                if(savedInitialCodecs!=scheme.initialCodecs) {
                    scheme.initialCodecs=savedInitialCodecs;
                    scheme.codecs=savedInitialCodecs.copy();
                }
                return (T)scheme.clone();
            } catch(Throwable t) {
            }
            return null;
        }
        
        /**
         * The codec is registered for the given type. Using the general access
         * keys, this implies restriction to access based on the class loader of the
         * core library.
         * 
         * @param type (one) object type of the type the codec is intended for.
         * @param codec the codec
         * @return this builder for additional configuration
         */
        public B addCodec(ObjectType type,Codec codec) {
            if(scheme.initialCodecs==savedInitialCodecs) {
                savedInitialCodecs=savedInitialCodecs.copy();
            }
            savedInitialCodecs.put(type, codec);
            return myself();
        }
        
        public B setTypeInfos(String mimeType,String extension) {
            scheme.mimeType=mimeType;
            scheme.fileExtension=extension;
            return myself();
        }

        public B setNamespace(String typeRef,Namespace ns) {
            scheme.namespace=ns;
            scheme.typeRef=typeRef;
            return myself();
        }

        public B setIndent(String indent,String lineTerminator) {
            scheme.indent=indent;
            scheme.lineTerminator=lineTerminator;
            return myself();
        }
        
        public B setRootType(TypeDefinition rootType) {
            scheme.rootType=rootType;
            return myself();
        }
        
        public B setCharset(Charset charset) {
            scheme.charset=charset;
            return myself();
        }
        
        public B setCharset(String charset) {
            return setCharset(Charset.forName(charset));
        }
        
        public B setReservedAttributes(ReservedAttributes attributes) {
            scheme.reservedAttributes=attributes;
            return myself();
        }
        
        public B setAccessFctory(AccessFactory factory) {
            // Codecs of different factories are different in general
            if(savedInitialCodecs==scheme.initialCodecs) {
                savedInitialCodecs=scheme.initialCodecs.copy();
            }
            scheme.factory=factory;
            return myself();
        }
        
        public T build() {
            return cloneScheme(scheme);
        }
        
        /**
         * Implement this to return <code>this</code>.
         * 
         * @return myself
         */
        public abstract B myself();
    }
    
    /**
     * Reserved attribute class containing values for the object reference and object id attributes. Subsequent schemes may extend this class to define more
     * attributes.
     * 
     * @author notalexa
     *
     */
    public static class ReservedAttributes {
        protected String objRef;
        protected String objId;
        
        public ReservedAttributes(String objRef,String objId) {
            this.objRef=objRef;
            this.objId=objId;
        }
        
        /**
         * 
         * @return the attribute for an object reference
         */
        public String getObjRefName() {
            return objRef;
        }
        
        /**
         * 
         * @return the attribute for an object id
         */
        public String getObjIdName() {
            return objId;
        }
    }
}
