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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

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
import not.alexa.netobjects.coding.xml.DeferredXMLObject.XMLNode;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder.FieldBuilder;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;

/**
 * Coding scheme for XML. In addition to the configuration defined in {@link AbstractTextCodingScheme}, this coding scheme defines the following additional configurations:
 * 
 * <ul>
 * <li><i>Reserved attributes:</i> Most schemes uses reserved attributes indicating special internal values. The XML coding scheme defines three reserved attributes:
 * <ul>
 * <li><code>obj-ref</code> denotes the reference for a (formerly) defined object.
 * <li><code>obj-id</code> denotes the reference for an object.
 * <li><code>is-empty</code> indicates an empty array.
 * </ul>
 * These attributes can be overridden using {@link Builder#setReservedAttributes(ReservedAttributes)} and retrieved using {@link #getReservedAttributes()}.
 * <li>An additional attribute <code>rootTag</code> configurable in the builder with {@link Builder#setRootTag(String)} and retrievable via {@link XMLCodingScheme#getRootTag()}
 * describes the XML root tag used for encoding. The default value is <code>object</code>.
 * <li>The scheme accepts all root tags (instead of just <code>rootTag</code>), if the attribute <code>acceptAllRootTags</code> is set to <code>true</code> via {@link Builder#setAcceptAllRootTags(boolean)}.
 * This is convenient for RESTful API's and set in {@link #REST_SCHEME}.
 * <li>The XML header will be written out if enabled by {@link Builder#enableHeader()} or {@link Builder#enableHeader(boolean)} (or setting a non null value for
 * the standalone pseudo attribute)
 * <li>The standalone pseudo attribute in the XML header can be set using the {@link Builder#setStandalone(String)} builder method. A non nul value will automatically enable
 * writing out the XML header.
 * </ul>
 * Beside the attributes, the tag for a field can be explicitly set via the {@link FieldBuilder#addTag(String, String)} method with first argument <code>XML</code>.
 * Special values starting with <code>@</code> denotes an attribute. The value <code>#text</code> should be used to indicate that the attribute should be
 * coded as a text node (where applicable, that is if the rest of data is encoded by attributes only).
 * <br>Namespaces are currently not supported.
 * 
 * @author notalexa
 *
 */
public class XMLCodingScheme extends AbstractTextCodingScheme implements CodingScheme {
	/**
	 * Writes data without an XML header
	 */
    public static final XMLCodingScheme INLINE_SCHEME=new XMLCodingScheme().newBuilder().setRootType(Object.class).build();
    
	/**
	 * Writes data with an XML header (but without the {@code standalone} pseudo attribute) and no object information at root.
	 * 
	 * @see Builder#enableHeader()
	 * @see Builder#setStandalone(String)
	 */
    public static final XMLCodingScheme REST_SCHEME=new XMLCodingScheme().newBuilder().setAcceptAllRootTags(true).enableHeader().build();

	/**
	 * Writes data with an XML header (but without the {@code standalone} pseudo attribute). Root object is {@code Object}.
	 * @see Builder#enableHeader()
	 * @see Builder#setStandalone(String)
	 */
    public static final XMLCodingScheme DEFAULT_SCHEME=REST_SCHEME.newBuilder().setAcceptAllRootTags(false).setRootType(Object.class).build();

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

    private boolean acceptAllRootTags;
    private String rootTag="object";
    private boolean enableHeader;
    private String standalone;
    private ReservedAttributes reservedAttributes=new ReservedAttributes("obj-ref","obj-id","is-empty");
	
	public static Charset defaultCharset() {
		try {
			return Charset.forName("UTF-8");
		} catch(Throwable t) {
			return Charset.defaultCharset();
		}
	}
	
	private XMLCodingScheme() {
		this(defaultCharset(),AccessFactory.getDefault());
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
		return new XMLEncoder(support,stream) {
			@Override
			public Encoder encode(Object o) throws BaseException {
				TypeDefinition rootType=getRootType(context, o.getClass());
				init(rootTag,rootType.isAbstract()?XMLEncoder.SHOWTYPE_MASK:0,support.getFactory().resolve(context,rootType));
				try {
					return super.encode(o);
				} finally {
					try {
						writer.flush();
					} catch(Throwable t) {
					}
				}
			}
		};
	}
	
	public boolean doAcceptAllRootTags() {
		return acceptAllRootTags;
	}

	@Override
	public Decoder createDecoder(Context context, InputStream stream) {
		return new XMLDecoder(new TextCodingSupport<>(this,context),stream);
	}

	public Decoder createDecoder(Context context, XMLNode node) {
		return new XMLDecoder(new TextCodingSupport<>(this,context),node);
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

    public ReservedAttributes getReservedAttributes() {
        return reservedAttributes;
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
        
        public Builder setAcceptAllRootTags(boolean acceptAllRootTags) {
            scheme.acceptAllRootTags=acceptAllRootTags;
        	return this;
        }
                
        public Builder setReservedAttributes(ReservedAttributes attributes) {
            scheme.reservedAttributes=attributes;
            return this;
        }

        /**
         * Enable writing the header
         * 
         * @return this builder
         */
        public Builder enableHeader() {
        	return enableHeader(true);
        }
        
        /**
         * Set enabling writing the header.
         * 
         * @param enable if {@code true} write the header
         * @return this builder
         */
        public Builder enableHeader(boolean enable) {
        	scheme.enableHeader=enable;
        	return this;
        }

        /**
         * Set the standalone pseudo attribute. No checks are made on the value. If not null, writing the
         * header will be automatically enabled.
         * 
         * @param standalone the value of the pseudo attribute (if {@code null}, the attribute will not be written (default))
         * @return ths builder
         */
        public Builder setStandalone(String standalone) {
        	if(standalone!=null) {
        		enableHeader();
        	}
        	scheme.standalone=standalone;
        	return this;
        }

        @Override
        public XMLCodingScheme build() {
            if(scheme.reservedAttributes==null) {
                scheme.reservedAttributes=new ReservedAttributes("obj-ref","obj-id","is-empty");
            }
            return super.build();
        }
	}

    public String getRootTag() {
        return rootTag;
    }
    
    @Override
    protected Codec createClassCodec(Context context, Access access) throws BaseException {
        if(access!=null) {
            return new AccessCodec(((ClassTypeDefinition)access.getType()).enableObjectRefs(),access).init(true, codecs);
        } else {
            return null;
        }
    }
    
	@Override
	protected Codec createArrayCodec(Access access,Codec componentCodec) throws BaseException {
		return createInternal(rootTag, access,componentCodec);
	}
		
	private static Codec createInternal(String tagName,Access access,Codec componentCodec) throws BaseException {	
		return new Codec() {
            @Override
            public void encode(Buffer buffer, Object t) throws BaseException {
                XMLEncoder c=((XMLEncoder)buffer).getChild().init(tagName,0,access);
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
		
	}

    
    public Access getRootDecoder(TextCodingSupport<XMLCodingScheme> root,Class<?> clazz) {
        TypeDefinition anonymous=new ClassTypeDefinition().createBuilder().addField(getRootTag(), getRootType(root.getContext(),clazz)).build();
        return new Access.AbstractAccess() {

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
            	TypeDefinition rootType=getRootType(root.getContext(),clazz);
            	if(rootType!=null) {
                    return root.getFactory().resolve(root.getContext(), rootType);
				} else {
					throw new BaseException(BaseException.BAD_REQUEST, "Access for "+clazz.getName()+" cannot be resolved.");
				}
            }
        };
    }


    static class AccessCodec implements Codec {
        Access access;
        boolean disableObjectRefs;
        XMLCodingExtraInfo info;
        Field[] fields;
        Codec[] fieldCodecs;
        AccessCodec(boolean enableObjectRefs,Access access) {
            this.access=access;
            info=getExtraInfo(access.getType());
            disableObjectRefs=!enableObjectRefs;
            fields=sort(access.getFields());
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
                Object t=access.getField(buffer,o, f);
                if(t!=null&&!f.isDefault(t)) {
                    Encoder child=buffer.push(info,null,f);
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
            return fieldCodecs[f.getIndex()];
        }
        
        private Codec init(boolean store,Codecs pool) {
        	if(store) {
        		pool.put(access, this);
        	}
        	fieldCodecs=new Codec[fields.length];
        	for(int i=0;i<fieldCodecs.length;i++) try {
        		Field f=fields[i];
        		Codec codec=null;
                switch(f.getType().getFlavour()) {
	                case InterfaceType:
	                case UnknownType:
	                case MethodType:
	                	break;
	                case PrimitiveType:codec=pool.get(access.getFieldAccess(f));
	                    break;
	                default:codec=createCodec(pool,f.getTag("XML"),f.getType(), access.getFieldAccess(f));
	                    break;
	            } 
	            fieldCodecs[f.getIndex()]=codec;
        	} catch(BaseException e) {
        		e.printStackTrace();
        	}
        	return this;
        }
        
        protected Codec createCodec(Codecs pool,String tagName,TypeDefinition type,Access access) throws BaseException {
            switch(type.getFlavour()) {
                case ArrayType:
                    Access componentAccess=access.getComponentAccess();
                    Codec componentCodec=createCodec(pool,tagName, ((ArrayTypeDefinition)type).getComponentType(), componentAccess);
                    return createInternal(tagName,access,componentCodec);
                case PrimitiveType:return pool.get(access);
                case EnumType:Codec codec=pool.get(access);
                    if(codec==null) {
                        codec=new EnumCodec(type.getJavaClassType());
                        pool.put(access, codec);
                    }
                    return codec;
                case ClassType:if(type.isAnonymous()) {
                        return new AccessCodec(false, access).init(false, pool);
                    } else {
                        codec=pool.get(access);
                        if(codec==null) {
                            codec=new AccessCodec(((ClassTypeDefinition)type).enableObjectRefs(),access).init(true, pool);
                        }
                    }
                    return codec;
                default:
                    return null;
            }
        }
    }
    
    public static class ReservedAttributes {
    	private String objRef;
        private String objId;
        private String isEmpty;
        public ReservedAttributes(String objRef, String objId,String isEmpty) {
            this.objRef=objRef;
            this.objId=objId;
            this.isEmpty=isEmpty;
        }
                
        public String getIsEmptyName() {
            return isEmpty;
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

            public void setField(AccessContext context,String f, AccessibleObject value) throws BaseException {
            	setField(context,get(f),value);
            }

            public void setField(AccessContext context,Field f, AccessibleObject value) throws BaseException {
                if(a[f.getIndex()]!=SET) {
                    checked++;
                    a[f.getIndex()]=SET; 
                }
                obj.setField(context,f, value);
            }
            
            public AccessibleObject checked(AccessContext context) throws BaseException {
                if(checked<checkCount) {
                    // Not all set. Mandatory field are present but maybe a default value exist.
                    for(int i=0;i<a.length;i++) {
                        if(a[i]!=SET) {
                            if(a[i]==MANDATORY||access==null) {
                                throw new BaseException(BaseException.BAD_REQUEST, "Field "+fields[i].getName()+" in "+fields[i].getClassDescription()+" is mandatory but not set.");
                            } else {
                                obj.setField(context,fields[i],access.getFieldAccess(fields[i]).makeDefault(context,a[i]));
                            }
                        }
                    }
                    checked=checkCount;
                }
                return obj;
            }
			public void putField(String qName, Field f) {
				fieldMap.put(qName, f);
			}
        }
    }

	public boolean writeHeader(Writer writer) throws IOException {
		if(enableHeader) {
			writer.append("<?xml version=\"1.0\" encoding=\"").append(getCodingCharset().name());
			(standalone==null?writer:writer.append("\" standalone=\"").append(standalone)).append("\"?>").append(getLineTerminator());
			return true;
		} else {
			return false;
		}
	}
}