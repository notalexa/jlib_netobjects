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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.text.EnumCodec;
import not.alexa.netobjects.types.JavaClass.Type;
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
 * <li><i>Character Set:</i> The character set used by this coding scheme can be configured using {@link Builder#setCharset(Charset)} or {@link Builder#setCharset(String)} and 
 * retrieved using {@link #getCodingCharset().
 * <li><i>Root type:</i> The root type of this coding scheme. This type is typically either a concrete class type (or primitive type) or an interface indicating that the
 * scheme understands a set of types classified by the interface type. The default root type is the primitive type "object" and can be configured using {@link Builder#setRootType(TypeDefinition)}
 * and retrieved using {@link #getRootType()}.
 * <li><i>Special codecs:</i> The scheme defines an initial set of codecs (for the primitive types). Special codecs can be added using the {@link Builder#addCodec(ObjectType, Codec)} method.
 * Typical examples are byte arrays or numbers, which should be hex encoded.
 * <li><i>Namespaces:</i> The namespace used by the coding scheme can be configured. By default, the namespace is the Java Namespace with type attribute <code>class</code> but this can be configured using {@link Builder#setNamespace(String, Namespace)}.
 * <li><i>Access Factories:</i> The scheme needs specific access to types for object creation and setting and getting attributes. This {@link Access} can be retrieved using the {@link AccessFactory}
 * which can be set using {@link Builder#setAccessFctory(AccessFactory)}.
 * <li><i>Resource Branches:</i> Text files have the important use case of configuring systems. To improve usability, if a resource branch is set and it's
 * supported by the coding scheme, resources can be defined for every class using the branch in the text file.
 * </ul>
 * @author notalexa
 *
 */
public abstract class AbstractTextCodingScheme extends AbstractCodingScheme implements CodingScheme, Cloneable {
    protected Charset charset;
    protected String indent="";
    protected String lineTerminator="";
    protected Set<String> disabledHints;
    protected String typeRef="class";
    protected String resourceBranch;
    protected Codecs codecs;
    protected AbstractTextCodingScheme(Charset charset,AccessFactory factory) {
    	super(factory,PrimitiveTypeDefinition.getTypeDescription(Object.class));
        this.charset=charset;
        codecs=Codecs.defaultTextCodecs();
    }
    
    @Override
    public Charset getCodingCharset() {
        return charset;
    }
    
    public String getIndent() {
        return indent;
    }
    
    public String getLineTerminator() {
        return lineTerminator;
    }
    
    public String getResourceBranch() {
        return resourceBranch;
    }
    
    public String getTypeRef() {
        return typeRef;
    }
    
    public Codec getCodec(Context context,ObjectType type,Access access) throws BaseException {
        if(access!=null) {
            Codec codec=codecs.get(access);
            if(codec==null) {
                switch(access.getType().getFlavour()) {
                    case PrimitiveType:throw new BaseException(BaseException.NOT_FOUND,"Primitive type "+access.getType()+" has no predefined codec.");
                    case InterfaceType:codec=createInterfaceCodec(context, type, access.getType());
                        break;
                    case ArrayType:codec=createArrayCodec(context, type, access);
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
                    codecs.put(access, codec);
                }
            }
            return codec;
        }
        return null;
    }

    protected Codec createInterfaceCodec(Context context,ObjectType type,TypeDefinition typeDef) throws BaseException {
    	return null;
    }
    
    protected Codec createMethodCodec(Context context,ObjectType type,TypeDefinition typeDef) throws BaseException {
        throw new BaseException(BaseException.NOT_FOUND,"Codecs for method types are not defined.");
    }

	protected Codec createArrayCodec(Context context, ObjectType type, Access access) throws BaseException {
		Access componentAccess=access.getComponentAccess();
		Codec componentCodec=getCodec(context, componentAccess.getType().getType(getNamespace()), componentAccess);
		return createArrayCodec(access,componentCodec);
	}	
		
	protected Codec createArrayCodec(Access access,Codec componentCodec) throws BaseException {
        throw new BaseException(BaseException.NOT_FOUND,"Codecs for array types are not defined.");
    }
    
    protected Codec createEnumCodec(Context context,ObjectType type,TypeDefinition typeDef) throws BaseException {
        return typeDef.getJavaClassType()==null?null:new EnumCodec(typeDef.getJavaClassType());
    }
    
    protected Codec createClassCodec(Context context,ObjectType type,Access access) throws BaseException {
        return null;
    }
    
    /**
     * Check if the given hint is enabled. Not that an enabled hint doesn't imply that the hint is supported in the given coding scheme.
     * 
     * @param hint the hint
     * @return {@code true} if the hint is enabled in this coding scheme.
     */
	public boolean isHintEnabled(String hint) {
		return !(disabledHints!=null&&disabledHints.contains(hint));
	}
    
    /**
     * Generic abstract builder class which can be extended by builders for non abstract coding schemes.
     * 
     * @author notalexa
     *
     * @param <T> the coding scheme class this builder creates
     * @param <B> the builder class itself
     */
    public static abstract class Builder<T extends AbstractTextCodingScheme,B extends Builder<T,B>> extends AbstractCodingScheme.Builder<T, B> {
        private Map<Type,Codec> primitiveTypeCodecs=new HashMap<>();
        private Map<Type,Codec> primitiveTypeCodecsInScheme=new HashMap<>();
        private Set<String> disabledHints;
        
        public Builder(T scheme) {
        	super(scheme);
        }
        
        private Set<String> getDisabledHints() {
        	if(disabledHints==null) {
        		disabledHints=scheme.disabledHints==null?new HashSet<String>():new HashSet<String>(scheme.disabledHints);
        	}
        	return disabledHints;
        }
        
        
        public T cloneScheme(T scheme) {
            try {
                if(primitiveTypeCodecs!=null&&primitiveTypeCodecs.size()>0) {
                    primitiveTypeCodecsInScheme.putAll(primitiveTypeCodecs);
                    scheme.codecs=scheme.codecs.copy(primitiveTypeCodecsInScheme);
                    primitiveTypeCodecs.clear();
                }
                return super.cloneScheme(scheme);
            } catch(Throwable t) {
            }
            return null;
        }
        
        public B disableCodingHints(String...hints) {
        	getDisabledHints().addAll(Arrays.asList(hints));
        	return myself();
        }

        public B enableCodingHints(String...hints) {
        	if(scheme.disabledHints!=null) {
        		getDisabledHints().removeAll(Arrays.asList(hints));
        	}
        	return myself();
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
        public B addCodec(Type type,Codec codec) {
            primitiveTypeCodecs.put(type,codec);
            return myself();
        }

        public B setNamespace(String typeRef,Namespace ns) {
            scheme.typeRef=typeRef;
            return super.setNamespace(ns);
        }

        public B setIndent(String indent,String lineTerminator) {
            scheme.indent=indent;
            scheme.lineTerminator=lineTerminator;
            return myself();
        }
        
        public B setCharset(Charset charset) {
            scheme.charset=charset;
            return myself();
        }
        
        public B setCharset(String charset) {
            return setCharset(Charset.forName(charset));
        }
        
        public B setResourceBranch(String resourceBranch) {
            scheme.resourceBranch=resourceBranch;
            return myself();
        }
        
        public B setAccessFctory(AccessFactory factory) {
        	primitiveTypeCodecsInScheme.putAll(primitiveTypeCodecs);
            scheme.codecs=scheme.codecs.copy(primitiveTypeCodecsInScheme);
            primitiveTypeCodecs.clear();
            return super.setAccessFctory(factory);
        }
        
        public T build(boolean initCodecs) {
        	Set<String> schemeHints=scheme.disabledHints==null?Collections.emptySet():scheme.disabledHints;
        	initCodecs|=disabledHints!=null&&!disabledHints.equals(schemeHints);
        	if(initCodecs) {
            	primitiveTypeCodecsInScheme.putAll(primitiveTypeCodecs);
                scheme.codecs=scheme.codecs.copy(primitiveTypeCodecsInScheme);
                primitiveTypeCodecs.clear();
                if(disabledHints!=null) {
                	scheme.disabledHints=disabledHints.size()>0?disabledHints:null;
                	disabledHints=null;
                }
        	}
            return super.build(initCodecs);
        }
    }
    
    /**
     * For internal use only.
     * 
     * @author notalexa
     *
     * @param <S> the type of the (text) coding scheme
     * @param <T> the extension of this class
     */
    public abstract static class TextCodingItem<S extends AbstractTextCodingScheme,T extends TextCodingItem<S,T>> {
        protected final TextCodingSupport<S> root;
        private T cachedChild;
        protected final T parent;
        protected Object fieldName;
        protected Access access;

        protected TextCodingItem(TextCodingSupport<S> root) {
            this.root=root;
            parent=null;
        }

        protected TextCodingItem(T parent) {
            this.root=parent.root;
            this.parent=parent;
        }

        protected T init(Object fieldName,Access access) {
            this.fieldName=fieldName;
            this.access=access;
            return null;
        }
        
        public  final T getChild() {
            if(cachedChild==null) {
                cachedChild=createChild();
            }
            return cachedChild;
        }
        
        protected abstract T createChild();
        
        protected Codec resolveCodec(ObjectType type,Access access) throws BaseException {
           Codec codec=root.getCodingScheme().getCodec(getContext(), type, access);
           if(codec==null) {
                  throw new BaseException(BaseException.NOT_FOUND,"Codec for "+type);
           }
           return codec;
        }
        
        public S getCodingScheme() {
            return root.getCodingScheme();
        }
        
        public Context getContext() {
            return root.getContext();
        }
        
        public TypeDefinition getType() {
            return access.getType();
        }
    }
}
