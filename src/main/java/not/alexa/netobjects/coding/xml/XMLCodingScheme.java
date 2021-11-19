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
package not.alexa.netobjects.coding.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;

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
import not.alexa.netobjects.coding.text.EnumCodec;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder.FieldBuilder;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessFactory;
import not.alexa.netobjects.types.access.MapEntryAccess;

/**
 * Coding scheme for XML. In addition to the configuration defined in {@link AbstractTextCodingScheme}, this coding scheme defines the following additional configurations:
 * 
 * <ul>
 * <li>An additional reserved attribute with default value <code>is-empty</code> defined in the reserved attribute extension {@link ReservedAttributes}. This attribute is used to
 * indicate an array without any values.
 * <li>An additional attribute <code>rootTag</code> configurable in the builder with {@link Builder#setRootTag(String)} and retrievable via {@link XMLCodingScheme#getRootTag()}
 * describes the XML root tag used for encoding. The default value is <code>object</code>.
 * </ul>
 * Beside the attributes, the tag for a field can be explizitly set via the {@link FieldBuilder#addTag(String, String)} method with first argument <code>XML</code>
 * Special values starting with <code>@</code> denotes an attribute. The value <code>#text</code> should be used to indicate that the attribute should be
 * coded as a text attribute (where applicable, that is if the attribute is a simple type).
 * <br>Namespaces are currently not supported.
 * 
 * @author notalexa
 *
 */
public class XMLCodingScheme extends AbstractTextCodingScheme implements CodingScheme {
    public static final XMLCodingScheme DEFAULT_SCHEME=new XMLCodingScheme();
    private static final Comparator<ClassTypeDefinition.Field> ATTRIBUTE_FIRST=new Comparator<ClassTypeDefinition.Field>() {

        @Override
        public int compare(ClassTypeDefinition.Field f0, ClassTypeDefinition.Field f1) {
            String s0=f0.getTag("XML");
            String s1=f1.getTag("XML");
            if(s0.length()>0&&s0.charAt(0)=='@') {
                if(s1.length()==0||s1.charAt(0)!='@') {
                    return -1;
                }
            }
            if(s1.length()>0&&s1.charAt(0)=='@') {
                if(s0.length()==0||s0.charAt(0)!='@') {
                    return 1;
                }
            }
            return f0.getIndex()-f1.getIndex();
        }
    };
    private static Field[] sort(Field[] fields) {
        fields=fields.clone();
        Arrays.sort(fields,ATTRIBUTE_FIRST);
        return fields;
    }

    private String rootTag="object";
    private ReservedAttributes reservedXMLAttributes=new ReservedAttributes(reservedAttributes);
	
	public static Charset defaultCharset() {
		try {
			return Charset.forName("UTF-8");
		} catch(Throwable t) {
			return Charset.defaultCharset();
		}
	}
	
	private XMLCodingScheme() {
		this(defaultCharset(),new DefaultAccessFactory());
	}
	
	public XMLCodingScheme(Charset charset,AccessFactory factory) {
	    super(charset,factory);
	    mimeType="text/xml";
	    fileExtension="xml";
	}
	
	@SuppressWarnings("resource")
	@Override
	public Encoder createEncoder(Context context, OutputStream stream) {
	    TextCodingSupport<XMLCodingScheme> support=new TextCodingSupport<XMLCodingScheme>(this,context);
		return new XMLEncoder(support,stream)
		        .init(rootTag,false,support.getFactory().resolve(context,rootType));
	}

	@Override
	public Decoder createDecoder(Context context, InputStream stream) {
		return new XMLDecoder(new TextCodingSupport<>(this,context),stream);
	}
    	
    @Override
    public ReservedAttributes getReservedAttributes() {
        return reservedXMLAttributes;
    }
	
	public Builder newBuilder() {
	    return new Builder(this);
	}
	
	public static Builder builder() {
	    return DEFAULT_SCHEME.newBuilder();
	}
	
	public static class Builder extends AbstractTextCodingScheme.Builder<XMLCodingScheme, Builder> {

        public Builder(XMLCodingScheme scheme) {
            super(scheme);
        }
        
        public Builder setRootTag(String rootTag) {
            scheme.rootTag=rootTag;
            return this;
        }
        
        public Builder setReservedAttributes(not.alexa.netobjects.coding.AbstractTextCodingScheme.ReservedAttributes attributes) {
            setReservedAttributes(new ReservedAttributes(attributes));
            return this;
        }
        
        public Builder setReservedAttributes(ReservedAttributes attributes) {
            scheme.reservedAttributes=scheme.reservedXMLAttributes=attributes;
            return this;
        }


        @Override
        public Builder myself() {
            return this;
        }

        @Override
        public XMLCodingScheme build() {
            if(scheme.reservedXMLAttributes==null) {
                scheme.reservedXMLAttributes=new ReservedAttributes(scheme.reservedAttributes);
            }
            return super.build();
        }
	}

    public String getRootTag() {
        return rootTag;
    }
    
    @Override
    protected Codec createClassCodec(Context context, ObjectType type, Access access) throws BaseException {
        if(access!=null) {
            return new AccessCodec(((ClassTypeDefinition)access.getType()).enableObjectRefs(),access,codecs);
        } else {
            return null;
        }
    }
    
    
    public Access getRootDecoder(TextCodingSupport<XMLCodingScheme> root) {
        TypeDefinition anonymous=new ClassTypeDefinition().createBuilder().addField(getRootTag(), getRootType()).build();
        return new Access() {

            @Override
            public TypeDefinition getType() {
                return anonymous;
            }

            @Override
            public AccessibleObject newAccessible(AccessContext context) throws BaseException {
                return null;
            }

            @Override
            public Access getFieldAccess(Field f) throws BaseException {
                return root.getFactory().resolve(root.getContext(), getRootType());
            }
            
        };
    }


    public static class AccessCodec implements Codec {
        Access access;
        Codecs pool;
        boolean disableObjectRefs;
        Field[] fields;
        Codec[] fieldCodecs;
        AccessCodec(boolean enableObjectRefs,Access access,Codecs pool) {
            this.access=access;
            disableObjectRefs=!enableObjectRefs;
            fields=sort(access.getFields());
            fieldCodecs=new Codec[fields.length];
            this.pool=pool;
        }
        @Override
        public void encode(Buffer buffer, Object o) throws BaseException {
            if(disableObjectRefs||!buffer.isReferenced(o)) for(Field f:fields) {
                Object t=access.getField(o, f);
                if(t!=null) {
                    Encoder child=buffer.push(f);
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
                    default:codec=createCodec(f.getTag("XML"),f.getType(), access.getFieldAccess(f));
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
                        XMLEncoder c=((XMLEncoder)buffer).getChild().init(tagName,false,access);
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
    
    public static class ReservedAttributes extends AbstractTextCodingScheme.ReservedAttributes {
        private String isEmpty;
        public ReservedAttributes(String objRef, String objId,String isEmpty) {
            super(objRef, objId);
            this.isEmpty=isEmpty;
        }
        
        public ReservedAttributes(AbstractTextCodingScheme.ReservedAttributes attributes) {
            this(attributes.getObjRefName(),attributes.getObjIdName(),"is-empty");
        }
        
        public String getIsEmptyName() {
            return isEmpty;
        }
    }
}