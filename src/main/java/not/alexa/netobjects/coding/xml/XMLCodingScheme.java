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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;

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
		        .init(rootTag,rootType.isAbstract()?XMLEncoder.SHOWTYPE_MASK:0,support.getFactory().resolve(context,rootType));
	}

	@Override
	public Decoder createDecoder(Context context, InputStream stream) {
		return new XMLDecoder(new TextCodingSupport<>(this,context),stream);
	}

	/**
	 * Additional decoder support for a node. Typically, this is a node in a bigger document where parts configures objects.
	 * 
	 * @param context the context to use
	 * @param node the node 
	 * @return a decoder for this node
	 */
	public Decoder createDecoder(Context context, Node node) {
		return new XMLDecoder(new TextCodingSupport<>(this,context),node);
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
            public AccessFactory getFactory() {
                return root.getFactory();
            }
            
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


    static class AccessCodec implements Codec {
        Access access;
        Codecs pool;
        boolean disableObjectRefs;
        XMLCodingExtraInfo info;
        Field[] fields;
        Codec[] fieldCodecs;
        AccessCodec(boolean enableObjectRefs,Access access,Codecs pool) {
            this.access=access;
            info=getExtraInfo(access.getType());
            disableObjectRefs=!enableObjectRefs;
            fields=sort(access.getFields());
            fieldCodecs=new Codec[fields.length];
            this.pool=pool;
        }
        
        @Override
        public void encode(Buffer buffer, Object o) throws BaseException {
            info.check();
            if(disableObjectRefs||!buffer.isReferenced(o)) {
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
                        XMLEncoder c=((XMLEncoder)buffer).getChild().init(tagName,type.isAbstract()?XMLEncoder.SHOWTYPE_MASK:0,access);
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
    
    static synchronized XMLCodingExtraInfo getExtraInfo(TypeDefinition type) {
        switch(type.getFlavour()) {
            case ClassType:XMLCodingExtraInfo info=type.getAdapter(XMLCodingExtraInfo.class);
                if(info==null) {
                    info=new XMLCodingExtraInfo((ClassTypeDefinition)type);
                    type.putAdapter(info);
                }
                return info;
            default:return XMLCodingExtraInfo.NULL_INFO;
        }
    }
    
    static class XMLCodingExtraInfo {
        private static final Object MANDATORY=new Object();
        private static final Object SET=new Object();
        private static final String[] NO_FIELDS=new String[0];
        static final XMLCodingExtraInfo NULL_INFO=new XMLCodingExtraInfo(null);
        private Field[] fields;
        private String[] attributes;
        private String[] tags;
        private Field textField;
        private Map<String,Field> fieldMap=new HashMap<>();
        private Object[] defaults;
        private String[] names;
        private int[] flags;
        private int checkCount;
        private BaseException codingException;
        private XMLCodingExtraInfo(ClassTypeDefinition type) {
            if(type==null) {
                attributes=tags=NO_FIELDS;
                textField=null;
                fieldMap=Collections.emptyMap();
                defaults=NO_FIELDS;
            } else {
                List<String> a=new ArrayList<>();
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
                        checkCount++;
                    } else if(f.isOptional()) {
                        // We don't care if the field is optional
                        defaults[i]=SET;
                    } else {
                        defaults[i]=MANDATORY;
                        checkCount++;
                    }
                    String name=f.getTag("XML");
                    names[i]=name;
                    if(name.length()==0) {
                        continue;
                    } else if("#text".equals(name)) {
                        if(textField==null) {
                            textField=f;
                            flags[i]|=2;
                        } else {
                            codingException=new BaseException(BaseException.BAD_REQUEST,"#text field declared twice.");
                            break;
                        }
                    } else if(name.charAt(0)=='@') {
                        String attr=name.substring(1);
                        names[i]=attr;
                        a.add(attr);
                        fieldMap.put(attr,f);
                        flags[i]|=1;
                    } else {
                        t.add(name);
                        fieldMap.put(name, f);
                    }
                }
                attributes=a.size()==0?NO_FIELDS:a.toArray(new String[a.size()]);
                tags=t.size()==0?NO_FIELDS:t.toArray(new String[t.size()]);
                if(codingException==null) {
                    if(textField!=null&&tags.length>0) {
                        codingException=new BaseException(BaseException.BAD_REQUEST,"Mixing tags and #text is forbidden.");
                    }
                }
            }
        }
        
        public void check() throws BaseException {
            if(codingException!=null) {
                throw codingException;
            }
        }
        
        public Field getTextField() {
            return textField;
        }
        
        public Field get(String name) {
            return fieldMap.get(name);
        }
        
        public String[] getAttributes() {
            return attributes;
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
        
        public Runtime createRuntime(AccessibleObject obj) {
            return createRuntime(null,obj);
        }
        
        public Runtime createRuntime(Access access,AccessibleObject obj) {
            return new Runtime(access,obj);
        }
        
        class Runtime {
            Access access;
            AccessibleObject obj;
            int checked;
            Object[] a=defaults.clone();
            private Runtime(Access access,AccessibleObject obj) {
                this.access=access;
                this.obj=obj;
            }
            public Field get(String name) {
                return XMLCodingExtraInfo.this.get(name);
            }
            
            public Field getTextField() {
                return textField;
            }
            
            public String[] getAttributes() {
                return XMLCodingExtraInfo.this.getAttributes();
            }
            
            public void setField(Field f, AccessibleObject value) throws BaseException {
                if(a[f.getIndex()]!=SET) {
                    checked++;
                    a[f.getIndex()]=SET; 
                }
                obj.setField(f, value);
            }
            
            public AccessibleObject checked() throws BaseException {
                if(checked<checkCount) {
                    // Not all set. Mandatory field are present but maybe a default value exist.
                    for(int i=0;i<a.length;i++) {
                        if(a[i]!=SET) {
                            if(a[i]==MANDATORY||access==null) {
                                throw new BaseException(BaseException.BAD_REQUEST, "Field "+fields[i].getName()+" in "+fields[i].getClassDescription()+" is mandatory but not set.");
                            } else {
                                obj.setField(fields[i],access.getFieldAccess(fields[i]).makeAccessible(a[i]));
                            }
                        }
                    }
                    checked=checkCount;
                }
                return obj;
            }
        }
    }
}