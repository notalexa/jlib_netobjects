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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import not.alexa.netobjects.Adaptable;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.AbstractTextCodingScheme.TextCodingItem;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.TextCodingSupport;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.Access.IllegalAccess;
import not.alexa.netobjects.types.access.Constructor;
import not.alexa.netobjects.utils.Sequence;

/**
 * Decoder implementation for XML.
 * 
 * @author notalexa
 *
 */
class XMLDecoder extends DefaultHandler implements Decoder {
	private static final SAXParserFactory FACTORY=SAXParserFactory.newInstance();
	protected InputStream stream;
	protected XMLContentHandler top;
	TextCodingSupport<XMLCodingScheme> root;
	
	public XMLDecoder(TextCodingSupport<XMLCodingScheme> root,InputStream stream) {
	    this.root=root;
		this.stream=stream;
	}

	@Override
	public <T> T decode(Class<T> clazz) throws BaseException {
		try {
			SAXParser parser=FACTORY.newSAXParser();
			top=new XMLContentHandler(this, root.getContext())
					.init("",root.getCodingScheme().getRootDecoder(root));
			parser.parse(stream, this);
			return (T)top.pop().castTo(root.getContext(), clazz);
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
	}

	@Override
	public void close() throws BaseException {
		if(stream!=null) try {
			stream.close();
		} catch(Throwable t) {
			BaseException.throwException(t);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		top=top.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			top=top.endElement(uri, localName, qName);
		} catch(BaseException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		top.characters(ch, start, length);
	}
	
	public static class XMLContentHandler extends TextCodingItem<XMLCodingScheme,XMLContentHandler> implements Decoder.Buffer {
	    StringBuilder content=new StringBuilder();
	    Field field;
	    AccessibleObject delegator;
	    Map<String,ArrayBuilder> arrays;
	    Adaptable.Default adaptable;

	    public XMLContentHandler(XMLDecoder decoder, Context context) {
	        super(decoder.root);
	        delegator=new AccessibleObject.Adapter() {
	            Object val;
	            
	            @Override
	            public Object getObject() {
	                return val;
	            }
	            
	            @Override
	            public TypeDefinition getType() {
	                return root.getCodingScheme().getRootType();
	            }
	            
	            @Override
	            public void setField(Field f, AccessibleObject value) throws BaseException {
	                this.val=value.getAssignable();
	            }
	        };
	    }

	    public XMLContentHandler(XMLContentHandler parent) {
	        super(parent);
	    }

	    public void characters(char[] ch, int start, int length) throws SAXException {
	        content.append(ch, start, length);
	    }

	    public XMLContentHandler endElement(String uri, String localName, String qName) throws SAXException,BaseException {
	        AccessibleObject o=pop();
	        parent.addField(field,o);
	        return parent;
	    }
	    
	    private XMLContentHandler start(Access fieldAccess,Field f,String qName,Attributes atts) throws BaseException {
	        int consumed=0;
	        Access tagAccess=fieldAccess;
	        TypeDefinition fieldType=fieldAccess.getType();
	        if(fieldType.getFlavour()==Flavour.ArrayType) {
	            defineArray(f,fieldAccess);
	            if("true".equals(atts.getValue(getCodingScheme().getReservedAttributes().getIsEmptyName()))) {
	                return new XMLContentHandler(this) {
	                    @Override
	                    public XMLContentHandler endElement(String uri, String localName, String qName) throws SAXException,BaseException {
	                        return parent;
	                    }           
	                };
	            }
	            fieldType=((ArrayTypeDefinition)fieldType).getComponentType();
	            tagAccess=fieldAccess.getComponentAccess();
	        }
	        XMLContentHandler handler=null;
	        ClassTypeDefinition classType=null;
	        boolean objRefs=false;
	        switch(fieldType.getFlavour()) {
	            case ClassType:classType=(ClassTypeDefinition)fieldType;
	                objRefs=classType.enableObjectRefs();
	            case InterfaceType:
	                String ref=atts.getValue(root.getCodingScheme().getReservedAttributes().getObjRefName());
	                if(ref!=null) try {
	                    AccessibleObject o=root.resolveObjectReference(ref);
	                    return getChild().init(qName,new IllegalAccess(fieldType)).setField(f,o);
	                } catch(Throwable t) {
	                    BaseException.throwException(t);
	                }
	                if(fieldType.getFlavour()==Flavour.InterfaceType) {
	                    String clazz=atts.getValue(root.getCodingScheme().getTypeRef());
	                    consumed++;
	                    try {
	                        fieldType=getContext().getTypeLoader().resolveType(getCodingScheme().getNamespace().create(clazz));
	                        tagAccess=root.getFactory().resolve(getContext(),fieldType);
	                    } catch(Throwable t) {
	                        return BaseException.throwException(t);
	                    }
	                    if(fieldType.getFlavour()==Flavour.ClassType) {
	                        classType=(ClassTypeDefinition)fieldType;
	                        objRefs=classType.enableObjectRefs();
	                    }
	                }
	        }
	        handler=getChild().init(qName,tagAccess).setField(f,tagAccess);
	        String ref=atts.getValue(root.getCodingScheme().getReservedAttributes().getObjIdName());
	        if(objRefs||ref!=null) {
	            if(ref!=null) {
	                consumed++;
	            }
	            root.addObjectReference(objRefs,ref,handler.delegator);                     
	        }
	        if(classType!=null&&atts.getLength()>consumed) for(Field f0:classType.getFields()) {
	            String name=f0.getTag("XML");
	            if(name.startsWith("@")) {
	                String value=atts.getValue(name.substring(1));
	                if(value!=null) {
	                    XMLContentHandler sub=handler.getChild().init(name,handler.access.getFieldAccess(f0)).setField(f0,handler.access.getFieldAccess(f0));
	                    sub.content.append(value);
	                    handler.addField(f0,sub.pop());
	                }
	            }
	        }
	        return handler;     
	    }

	    private void defineArray(Field f,Access access) throws BaseException {
	        if(arrays==null) {
	            arrays=new HashMap<>();
	        }
	        ArrayBuilder builder=arrays.get(f.getName());
	        if(builder==null) {
	            arrays.put(f.getName(),builder=new ArrayBuilder(f,access.newAccessible(this)));
	        }
	    }

	    public XMLContentHandler startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
	        TypeDefinition type=getType();
	        if(type.getFlavour()==Flavour.ClassType) for(Field f:((ClassTypeDefinition)type).getFields()) try {
	            if(qName.equals(f.getTag("XML"))) {
	                Access fieldAccess=access.getFieldAccess(f);
	                if(fieldAccess==null) {
	                    throw new BaseException();
	                }
	                return start(fieldAccess,f,qName,atts);
	            }
	        } catch(BaseException e) {
	            throw new SAXException(e);
	        } else if(type.getFlavour()==Flavour.ArrayType) {
	            if(qName.equals(field.getName())) try {
	                // Inner array (depth>1)
	                Field field=new ClassTypeDefinition().createBuilder().addField(qName, type).build().getFields()[0];
	                return start(access,field,qName,atts);
	            } catch(Exception e) {
	                throw new SAXException(e);
	            }
	        }
	        throw new SAXException();
	    }

	    @Override
	    protected XMLContentHandler createChild() {
	        return new XMLContentHandler(this);
	    }

	    @Override
	    public XMLContentHandler init(String fieldName, Access access) {
	        super.init(fieldName,access);
	        content.setLength(0);
	        if(parent!=null) {
	            delegator=null;
	        }
	        field=null;
	        arrays=null;
	        return this;
	    }
	    
        public XMLContentHandler setField(Field f,AccessibleObject o) throws BaseException {
            this.field=f;
            this.delegator=o;
            return this;
        }
	    
	    public XMLContentHandler setField(Field f,Access access) throws BaseException {
	        this.field=f;
	        this.access=access;
	        TypeDefinition resolvedType=access.getType();
	        switch(resolvedType.getFlavour()) {
	            case ClassType:
	                delegator=access.newAccessible(this);
	                break;
	            case ArrayType:
	                defineArray(f, access);
	                break;
	            default:
	                break;
	        }
	        return this;
	    }

	    @Override
	    public byte[] getByteContent() {
	        throw new RuntimeException();
	    }

	    @Override
	    public CharSequence getCharContent() {
	        return content;
	    }
	    
	    protected void addField(Field f,AccessibleObject o) throws BaseException {
	        if(f.getType().getFlavour()==Flavour.ArrayType) {
	            ArrayBuilder builder=arrays==null?null:arrays.get(f.getName());
	            if(builder!=null) {
	                builder.add(o);
	            } else {
	                throw new BaseException(BaseException.BAD_REQUEST,"Array "+f.getName()+" not initialized.");
	            }
	        } else if(delegator!=null) try {
	            delegator.setField(f, o);
	        } catch(Throwable t) {
	        }
	    }
	    
	    AccessibleObject pop() throws BaseException {
	        TypeDefinition type=getType();
	        if(delegator!=null) {
	            if(arrays!=null) for(ArrayBuilder a:arrays.values()) {
	                a.store(delegator);
	            }
	            arrays=null;
	            return delegator;
	        } else if(type.getFlavour()==Flavour.ArrayType) {
	            return arrays.get(field.getName());
	        } else try {
	            Codec codec=resolveCodec(type.getJavaClassType(),new Access.SimpleTypeAccess(type));
	            return new Access.SimpleTypeAccess(type,codec.decode(this));
	        } catch(Throwable t) {
	            return BaseException.throwException(t);
	        }
	    }
	    
	    private static class ArrayBuilder implements AccessibleObject {
	        Field f;
	        AccessibleObject obj;
	        public TypeDefinition getType() {
	            return obj.getType();
	        }

	        public Object getObject() {
	            return obj.getObject();
	        }

	        @Override
	        public void setField(Field f, AccessibleObject value) throws BaseException {
	            obj.add(value);
	        }

	        public AccessibleObject getField(Field f) throws BaseException {
	            return obj.getField(f);
	        }

	        public ArrayBuilder(Field f,AccessibleObject obj) {
	            this.f=f;
	            this.obj=obj;
	        }
	        
	        public void add(AccessibleObject o) throws BaseException {
	            obj.add(o);
	        }
	        
	        public void store(AccessibleObject o) throws BaseException {
	            switch(o.getType().getFlavour()) {
	                case ArrayType:o.add(obj);
	                    break;
	                default: o.setField(f,obj);
	                    break;
	            }
	        }

	        @Override
	        public Sequence<AccessibleObject> asSequence() {
	            return obj.asSequence();
	        }

	        @Override
	        public Object getAssignable() throws BaseException {
	            return obj.getAssignable();
	        }
	    }

	    @Override
	    public <T> T castTo(Context context, Class<T> clazz) {
	        if(delegator!=null) {
	            T t=delegator.castTo(context, clazz);
	            if(t!=null) {
	                return t;
	            }
	        }
	        return parent==null?context.castTo(context, clazz):parent.castTo(context, clazz);
	    }

        @Override
        public Access resolve(Context context, TypeDefinition type) {
            return root.getFactory().resolve(context, type);
        }

        @Override
        public Constructor resolve(Context context, Type type) {
            return root.getFactory().resolve(context, type);
        }

        @Override
        public Access resolve(Access referrer, TypeDefinition type) {
            return root.getFactory().resolve(referrer, type);
        }
	}

}
