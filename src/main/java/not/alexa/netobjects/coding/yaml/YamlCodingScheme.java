/*
 * Copyright (C) 2023 Not Alexa
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
package not.alexa.netobjects.coding.yaml;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.AbstractTextCodingScheme;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Codecs;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.Encoder.Buffer;
import not.alexa.netobjects.coding.TextCodingSupport;
import not.alexa.netobjects.coding.json.JsonCodingScheme;
import not.alexa.netobjects.coding.text.EnumCodec;
import not.alexa.netobjects.coding.yaml.Yaml.Document;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessFactory;
import not.alexa.netobjects.types.access.MapEntryAccess;

/**
 * Coding scheme for YAML. This includes indented mode and flow mode (that is JSON). For clearity, we introduced a formal setup for JSON modes in {@link JsonCodingScheme}
 * but the technology behind is this implementation.
 * <br>Most features are described in {@link Yaml}. For any instance, a coding scheme can be created using {@link YamlCodingScheme#YamlCodingScheme(Yaml)}. The following
 * default schemata are defined:
 * <ul>
 * <li>{@link #DEFAULT_SCHEME} with no scripts (and mode {@link Mode#Indented}).
 * <li>{@link #CONFIGURATION_SCHEME} with typical configuration scripts included.
 * </ul>
 * 
 * 
 * @author notalexa
 *
 */
public class YamlCodingScheme extends AbstractTextCodingScheme implements CodingScheme {
	/**
	 * Default instance for decoding YAML.
	 */
    public static final YamlCodingScheme DEFAULT_SCHEME=new YamlCodingScheme(new Yaml(Mode.Indented));
    /**
     * Default instance for decoding YAML as a configuration file. This includes the {@link IncludeScript}.
     */
    public static final YamlCodingScheme CONFIGURATION_SCHEME=new YamlCodingScheme(new Yaml(Mode.Indented,new IncludeScript()));
    
    /**
     * 
     * @return the default charset for YAML which is UTF-8
     */
	public static Charset defaultCharset() {
		try {
			return Charset.forName("UTF-8");
		} catch(Throwable t) {
			return Charset.defaultCharset();
		}
	}
	
	Yaml yaml;

	/**
	 * Construct a coding scheme with the given yaml instance
	 * @param yaml the yaml configuration
	 */
	public YamlCodingScheme(Yaml yaml) {
		this(yaml,defaultCharset(),new DefaultAccessFactory());
	}

	public YamlCodingScheme(Yaml yaml,Charset charset,AccessFactory factory) {
	    super(charset,factory);
	    this.yaml=yaml;
	    mimeType=yaml.mode==Mode.Indented?"text/yaml":"text/json";
	    fileExtension=yaml.mode==Mode.Indented?"yaml":"json";
	}
	
	@SuppressWarnings("resource")
	@Override
	public Encoder createEncoder(Context context, OutputStream stream) {
	    TextCodingSupport<YamlCodingScheme> support=new TextCodingSupport<YamlCodingScheme>(this,context);
		return new YamlEncoder(support,yaml.createOutput(getIndent(),getLineTerminator(), stream))
		        .init("ROOT",rootType.isAbstract()?YamlEncoder.SHOWTYPE_MASK:0,support.getFactory().resolve(context,rootType));
	}

	@Override
	public Decoder createDecoder(Context context, InputStream stream) {
		return new YamlDecoder(new TextCodingSupport<>(this,context),stream);
	}
	
	/**
	 * Additional decoder for YAML docs.
	 * 
	 * @param context the context to use for decoding
	 * @param doc the document to decode
	 * @return a decoder for the given input
	 */
	public Decoder createDecoder(Context context, Document doc) {
		return new YamlDecoder(new TextCodingSupport<>(this,context),doc);
	}

	/**
	 * Additional decoder for the given token.
	 * 
	 * @param context the context to use for decoding
	 * @param t the token to decode
	 * @return a decoder for the given input
	 * @throws YamlException if the token doesn't represent a node
	 */
	public Decoder createDecoder(Context context, Token t) throws YamlException {
		return createDecoder(context,yaml.asDocument(t));
	}

	public Builder newBuilder() {
	    return new Builder(this);
	}
	
	public static Builder builder() {
	    return DEFAULT_SCHEME.newBuilder();
	}
	
	public static class Builder extends AbstractTextCodingScheme.Builder<YamlCodingScheme, Builder> {

        public Builder(YamlCodingScheme scheme) {
            super(scheme);
        }

        @Override
        public Builder myself() {
            return this;
        }
	}
    
    @Override
    protected Codec createClassCodec(Context context, ObjectType type, Access access) throws BaseException {
        if(access!=null) {
            return new AccessCodec(((ClassTypeDefinition)access.getType()).enableObjectRefs(),access,codecs);
        } else {
            return null;
        }
    }
    
	public Access getRootDecoder(TextCodingSupport<YamlCodingScheme> root) {
		return root.getFactory().resolve(root.getContext(), getRootType());
    }


    static class AccessCodec implements Codec {
        Access access;
        Codecs pool;
        YAMLCodingExtraInfo info;
        Field[] fields;
        Codec[] fieldCodecs;
        AccessCodec(boolean enableObjectRefs,Access access,Codecs pool) {
            this.access=access;
            info=getExtraInfo(access.getType());
            fields=access.getFields();
            fieldCodecs=new Codec[fields.length];
            this.pool=pool;
        }
        
        @Override
        public void encode(Buffer buffer, Object o) throws BaseException {
            info.check();
            if(!buffer.isReferenced(o)) {
                for(Field f:fields) {
                    encode(buffer,o,f);
                }
            }
        }
        
        private void encode(Buffer buffer,Object o,Field f) throws BaseException {
            if(f!=null) {
                Object t=access.getField(o, f);
                if(t!=null&&(f.getDefaultValue()==null||!f.getDefaultValue().equals(t))) {
                    Encoder child=buffer.push(info,f);
                    child.encode(t);
                }
            }
        }
        
        @Override
        public Object decode(not.alexa.netobjects.coding.Decoder.Buffer buffer) throws BaseException {
            return access.newAccessible(buffer);
        }
        
        @Override
        public Codec getCodec(Field f) throws BaseException {
            Codec codec=fieldCodecs[f.getIndex()];
            if(codec==null) {
                switch(f.getType().getFlavour()) {
                    case InterfaceType:
                    case UnknownType:
                    case MethodType:return null;
                    case PrimitiveType:codec=pool.get(access.getFieldAccess(f));
                        break;
                    default:codec=createCodec(f.getTag("YAML"),f.getType(), access.getFieldAccess(f));
                        break;
                }
                fieldCodecs[f.getIndex()]=codec;
            }
            return codec;
        }
        
        protected Codec createCodec(String tagName,TypeDefinition type,Access access) throws BaseException {
            switch(type.getFlavour()) {
                case ArrayType:return new Codec() {
                    Access componentAccess=access.getComponentAccess();
                    Codec componentCodec=createCodec(tagName, ((ArrayTypeDefinition)type).getComponentType(), componentAccess);
                    @Override
                    public void encode(Buffer buffer, Object t) throws BaseException {
                        YamlEncoder c=((YamlEncoder)buffer).getChild().init(tagName,1|(type.isAbstract()?YamlEncoder.SHOWTYPE_MASK:0),access);
                        c.codec=this;
                        c.encode(t);
                    }
    
                    @Override
                    public Object decode(not.alexa.netobjects.coding.Decoder.Buffer buffer) throws BaseException {
                        return null;
                    }
    
                    @Override
                    public Codec getComponentCodec() throws BaseException {
                        return componentCodec;
                    }
                };
                case PrimitiveType:return pool.get(access);
                case EnumType:Codec codec=pool.get(access);
                    if(codec==null) {
                        codec=new EnumCodec(type.getJavaClassType());
                        pool.put(access, codec);
                    }
                    return codec;
                case ClassType:if(type.isAnonymous()) {
                        if(access instanceof MapEntryAccess) {
                            return new AccessCodec(false, access, pool);
                        } else {
                            throw new BaseException(BaseException.BAD_REQUEST,"Anonymous type not defining a map");
                        }
                    } else {
                        codec=pool.get(access);
                        if(codec==null) {
                            codec=new AccessCodec(((ClassTypeDefinition)type).enableObjectRefs(),access, pool);
                            pool.put(access, codec);
                        }
                    }
                    return codec;
                default:
                    return null;
            }
        }
    }
    
    static synchronized YAMLCodingExtraInfo getExtraInfo(TypeDefinition type) {
        switch(type.getFlavour()) {
            case ClassType:YAMLCodingExtraInfo info=type.getAdapter(YAMLCodingExtraInfo.class);
                if(info==null) {
                    info=new YAMLCodingExtraInfo((ClassTypeDefinition)type);
                    type.putAdapter(info);
                }
                return info;
            default:return YAMLCodingExtraInfo.NULL_INFO;
        }
    }
    
    static class YAMLCodingExtraInfo {
        private static final Object MANDATORY=new Object();
        private static final Object SET=new Object();
        private static final String[] NO_FIELDS=new String[0];
        private static final YAMLCodingExtraInfo NULL_INFO=new YAMLCodingExtraInfo(null);
        private Field[] fields;
        private String[] tags;
        private Map<String,Field> fieldMap=new HashMap<>();
        private Object[] defaults;
        private String[] names;
        private int[] flags;
        private BaseException codingException;
        private YAMLCodingExtraInfo(ClassTypeDefinition type) {
            if(type==null) {
                fieldMap=Collections.emptyMap();
                defaults=NO_FIELDS;
            } else {
                List<String> t=new ArrayList<>();
                fields=type.getFields();
                defaults=new Object[fields.length];
                flags=new int[fields.length];
                names=new String[fields.length];
                for(int i=0;i<fields.length;i++) {
                    Field f=fields[i];
                    flags[i]=f.isAbstract()?4:0;
                    if(f.getDefaultValue()!=null) {
                        defaults[i]=f.getDefaultValue();
                    } else if(f.isOptional()) {
                        // We don't care if the field is optional
                        defaults[i]=SET;
                    } else {
                        defaults[i]=MANDATORY;
                    }
                    String name=f.getTag("YAML");
                    names[i]=name;
                    if(name.length()==0) {
                        continue;
                    } else {
                        t.add(name);
                        fieldMap.put(name, f);
                    }
                }
                tags=t.size()==0?NO_FIELDS:t.toArray(new String[t.size()]);
            }
        }
        
        public void check() throws BaseException {
            if(codingException!=null) {
                throw codingException;
            }
        }
        
        public Field get(String name) {
            return fieldMap.get(name);
        }
        
        public String[] getTags() {
            return tags;
        }
        
        public int getFlags(Field f) {
            return flags[f.getIndex()];
        }
        
        public String getName(Field f) {
            return names[f.getIndex()];
        }
   }
}