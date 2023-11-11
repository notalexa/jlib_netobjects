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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.AbstractTextCodingScheme.TextCodingItem;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.TextCodingSupport;
import not.alexa.netobjects.coding.xml.XMLCodingScheme.XMLCodingExtraInfo;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
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
	protected Node node;
	protected InputStream stream;
	protected XMLContentHandler top;
	protected TextCodingSupport<XMLCodingScheme> root;
	protected boolean parsed;
	
	public XMLDecoder(TextCodingSupport<XMLCodingScheme> root,Node node) {
	    this.root=root;
		this.node=node;
	}
	
	public XMLDecoder(TextCodingSupport<XMLCodingScheme> root,InputStream stream) {
	    this.root=root;
		this.stream=stream;
	}

	@Override
	public <T> T decode(Class<T> clazz) throws BaseException {
		try {
			top=new XMLContentHandler(this, root.getContext())
					.init("",root.getCodingScheme().getRootDecoder(root));
			if(stream!=null) {
				SAXParser parser=FACTORY.newSAXParser();
				parser.parse(stream, this);
			} else if(node!=null) {
				fireEvents(node);
			}
			return (T)top.pop().castTo(root.getContext(), clazz);
		} catch(Throwable t) {
			return BaseException.throwException(t);
		} finally {
			parsed=true;
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
	public boolean hasNext() {
		return !parsed;
	}
	
	private void fireEvents(Node node) throws SAXException {
		switch(node.getNodeType()) {
			case Node.CDATA_SECTION_NODE:
			case Node.TEXT_NODE:
				char[] val=node.getNodeValue().toCharArray();
				characters(val, 0,val.length);
				break;
			case Node.ELEMENT_NODE:
				String uri=node.getNamespaceURI();
				String localName=node.getLocalName();
				String name=node.getNodeName();
				startElement(uri, localName, name, new NodeAttributes(node.getAttributes()));
				if(node.hasChildNodes()) for(Node child=node.getFirstChild();child!=null;child=child.getNextSibling()) {
					fireEvents(child);
				}
				endElement(uri, localName, name);
				break;
			case Node.DOCUMENT_NODE:
				if(node.hasChildNodes()) for(Node child=node.getFirstChild();child!=null;child=child.getNextSibling()) {
					fireEvents(child);
				}
			default:
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
	
	private static class XMLContentHandler extends TextCodingItem<XMLCodingScheme,XMLContentHandler> implements Decoder.Buffer {
	    private StringBuilder content=new StringBuilder();
	    private XMLCodingExtraInfo.Runtime current;
	    private Map<String,ArrayBuilder> arrays;

	    private XMLContentHandler(XMLDecoder decoder, Context context) {
	        super(decoder.root);
	    }

	    private XMLContentHandler(XMLContentHandler parent) {
	        super(parent);
	    }

	    public void characters(char[] ch, int start, int length) throws SAXException {
	        content.append(ch, start, length);
	    }

	    public XMLContentHandler endElement(String uri, String localName, String qName) throws SAXException,BaseException {
	        AccessibleObject o=pop();
	        return parent.addField(qName,o);
	    }
	    
	    private XMLContentHandler start(Access fieldAccess,boolean array,String qName,Attributes atts) throws BaseException {
	    	String objRef=atts.getValue(root.getCodingScheme().getReservedAttributes().getObjRefName());
            if(objRef!=null) try {
                AccessibleObject o=root.resolveObjectReference(objRef);
                if(o!=null) {
                	if(array) {
                		defineArray(qName,fieldAccess);
                	}
                	return new SkipHandler(addField(qName,o));
                } else {
    				throw new BaseException(BaseException.NOT_FOUND,"Undefined reference: "+objRef);
                }
            } catch(Throwable t) {
                BaseException.throwException(t);
            }
	        int consumed=0;
	        Access tagAccess=fieldAccess;
	        TypeDefinition fieldType=fieldAccess.getType();
	        if(fieldType.getFlavour()==Flavour.ArrayType) {
	            defineArray(qName,fieldAccess);
	            if("true".equals(atts.getValue(getCodingScheme().getReservedAttributes().getIsEmptyName()))) {
	            	return new SkipHandler(this);
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
	                break;
	            case InterfaceType:
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
                    break;
	        }
	        handler=getChild().init(qName,tagAccess).setField(qName,tagAccess);
	        handler.handleAttributes(atts, consumed,objRefs);
	        return handler;     
	    }
	    
	    private void handleAttributes(Attributes atts,int consumed,boolean objRefs) throws BaseException {
	        if(current!=null) {
		        String ref=atts.getValue(root.getCodingScheme().getReservedAttributes().getObjIdName());
	        	if(objRefs||ref!=null) {
		            if(ref!=null) {
		                consumed++;
		            }
		            root.addObjectReference(objRefs,ref,current.obj);                     
		        }
		        if(atts.getLength()>consumed) for(String attr:current.getAttributes()) {
		            String value=atts.getValue(attr);
	                if(value!=null) {
	                    Field f=current.get(attr);
	                    XMLContentHandler sub=getChild().init(attr,access.getFieldAccess(f)).setField(attr,access.getFieldAccess(f));
	                    sub.content.append(value);
	                    // Not an array
	                    current.setField(f, sub.pop());
		            }
		        }
	        }
	    }

	    private ArrayBuilder defineArray(String fieldName,Access access) throws BaseException {
	        if(arrays==null) {
	            arrays=new LinkedHashMap<>();
	        }
	        ArrayBuilder builder=arrays.get(fieldName);
	        if(builder==null) {
	            arrays.put(fieldName,builder=new ArrayBuilder(access.newAccessible(this)));
	        }
	        return builder;
	    }

	    public XMLContentHandler startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
	        if(current!=null) try {
	            Field f=current.get(qName);
	            if(f!=null) {
                    Access fieldAccess=access.getFieldAccess(f);
                    if(fieldAccess==null) {
                        throw new BaseException();
                    }
                    return start(fieldAccess,f.getType().getFlavour()==Flavour.ArrayType,qName,atts);	                
	            }
            } catch(BaseException e) {
                throw new SAXException(e);
	        } else {
	        	TypeDefinition type=getType();
	        	if(type.getFlavour()==Flavour.ArrayType) {
		            if(qName.equals(fieldName)) try {
		                // Inner array (depth>1)
		                return start(access,true,qName,atts);
		            } catch(Exception e) {
		                throw new SAXException(e);
		            }
		        }
	        }
	        if(qName.equals(getCodingScheme().getResourceBranch())) {
	        	String objId=atts.getValue(root.getCodingScheme().getReservedAttributes().getObjIdName());
	        	String clazz=atts.getValue(root.getCodingScheme().getTypeRef());
	        	if(objId!=null&&clazz!=null) try {
	        		Access objectAccess=getCodingScheme().getFactory().resolve(getContext(), PrimitiveTypeDefinition.getTypeDescription(Object.class));
	        		XMLContentHandler myHandler=new XMLContentHandler(this) {

						@Override
						XMLContentHandler addField(String fieldName, AccessibleObject o) throws BaseException {
							root.addObjectReference(false, objId, o);
							return parent;
						}
						
	        		}.init("item", objectAccess);
	        		XMLContentHandler resolved=myHandler.start(objectAccess, false, "item", atts);
	        		return resolved;
               } catch(Throwable t) {
            	   t.printStackTrace();
               }
        	}
	        return new SkipHandler(this);
	    }

	    @Override
	    protected XMLContentHandler createChild() {
	        return new XMLContentHandler(this);
	    }

	    @Override
	    protected XMLContentHandler init(String fieldName, Access access) {
	        super.init(fieldName,access);
	        content.setLength(0);
	        if(parent!=null) {
	            current=null;
	        } else {
	            current=XMLCodingScheme.getExtraInfo(access.getType()).createRuntime(/*delegator=*/new AccessibleObject.Adapter() {
	                Object val;
	                
	                @Override
	                public Object getObject() {
	                    return val;
	                }
	                
	                @Override
	                public TypeDefinition getType() {
	                    return access.getType();
	                }
	                
	                @Override
	                public void setField(Field f, AccessibleObject value) throws BaseException {
	                    this.val=value.getAssignable();
	                }
	            });
	        }
	        arrays=null;
	        return this;
	    }
	    
	    private XMLContentHandler setField(String tagName,Access access) throws BaseException {
	        this.fieldName=tagName;
	        this.access=access;
	        TypeDefinition resolvedType=access.getType();
	        switch(resolvedType.getFlavour()) {
	            case ClassType:
	                current=XMLCodingScheme.getExtraInfo(resolvedType).createRuntime(access,access.newAccessible(this));
	                break;
	            case ArrayType:
	                defineArray(tagName, access);
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
	    
	    XMLContentHandler addField(String fieldName,AccessibleObject o) throws BaseException {
            ArrayBuilder builder=arrays==null?null:arrays.get(fieldName);
            if(builder!=null) {
                builder.add(o);
            } else {
	            current.setField(fieldName, o);
            }
            return this;
	    }
	    
	    AccessibleObject pop() throws BaseException {
	        TypeDefinition type=getType();
            if(current!=null) {
                if(arrays!=null) for(Map.Entry<String,ArrayBuilder> entry:arrays.entrySet()) {
                    current.setField(entry.getKey(),entry.getValue().obj);
                }
                Field textField=current.getTextField();
   	            if(textField!=null) {
   	                Access simpleAccess=new Access.SimpleTypeAccess(getCodingScheme().getFactory(),textField.getType());
   	                Codec codec=resolveCodec(textField.getType().getJavaClassType(),simpleAccess);
   	                current.setField(textField, simpleAccess.makeAccessible(codec.decode(this)));
    	        }
   	            return current.checked();
	        } else if(type.getFlavour()==Flavour.ArrayType) {
	            return arrays.get(fieldName).obj;
	        } else try {
	            Access simpleAccess=new Access.SimpleTypeAccess(getCodingScheme().getFactory(),type);
	            Codec codec=resolveCodec(type.getJavaClassType(),simpleAccess);
	            return simpleAccess.makeAccessible(codec.decode(this));
	        } catch(Throwable t) {
	            return BaseException.throwException(t);
	        }
	    }
	    
	    private static class ArrayBuilder implements AccessibleObject {
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

	        public ArrayBuilder(AccessibleObject obj) {
	            this.obj=obj;
	        }
	        
	        public void add(AccessibleObject o) throws BaseException {
	            obj.add(o);
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
	        if(current!=null) {
	            T t=current.obj.castTo(context, clazz);
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

	static class NodeAttributes implements Attributes {
		NamedNodeMap map;
		NodeAttributes(NamedNodeMap map) {
			this.map=map;
		}
		@Override
		public int getLength() {
			return map.getLength();
		}

		@Override
		public String getURI(int index) {
			return map.item(index).getNamespaceURI();
		}

		@Override
		public String getLocalName(int index) {
			return map.item(index).getLocalName();
		}

		@Override
		public String getQName(int index) {
			return map.item(index).getNodeName();
		}

		@Override
		public String getType(int index) {
			return "CDATA";
		}

		@Override
		public String getValue(int index) {
			return map.item(index).getNodeValue();
		}

		@Override
		public int getIndex(String uri, String localName) {
			Node n=map.getNamedItemNS(uri,localName);
			for(int i=0;i<map.getLength();i++) {
				if(map.item(i)==n) {
					return i;
				}
			}
			return -1;
		}

		@Override
		public int getIndex(String qName) {
			Node n=map.getNamedItem(qName);
			for(int i=0;i<map.getLength();i++) {
				if(map.item(i)==n) {
					return i;
				}
			}
			return -1;
		}

		@Override
		public String getType(String uri, String localName) {
			return "CDATA";
		}

		@Override
		public String getType(String qName) {
			return "CDATA";
		}

		@Override
		public String getValue(String uri, String localName) {
			Node n=map.getNamedItemNS(uri,localName);
			return n==null?null:n.getNodeValue();
		}

		@Override
		public String getValue(String qName) {
			Node n=map.getNamedItem(qName);
			return n==null?null:n.getNodeValue();
		}
	}
	
	private static class SkipHandler extends XMLContentHandler {
		private int depth=1;
		
        private SkipHandler(XMLContentHandler parent) {
			super(parent);
		}

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        @Override
        public XMLContentHandler endElement(String uri, String localName, String qName) throws SAXException, BaseException {
            depth--;
            return depth==0?parent:this;
        }

        @Override
        public XMLContentHandler startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            depth++;
            return this;
        }

        @Override
        AccessibleObject pop() throws BaseException {
            return null;
        }
	}
}
