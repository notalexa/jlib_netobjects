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
package not.alexa.netobjects.coding.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Castable;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.xml.XMLEncoder.XMLNodeHolder;
import not.alexa.netobjects.types.Deferred;
import not.alexa.netobjects.types.DeferredObject;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;

/**
 * Class representing a deferred object from a XML source.
 * 
 * @author notalexa
 */
class DeferredXMLObject extends DeferredObject implements XMLNodeHolder {
	ObjectType type;
	Context context;
	XMLCodingScheme scheme;
	XMLNode node;
	
	public DeferredXMLObject() {
	}

	public DeferredXMLObject(Object o) {
		super(o);
	}
	
	public DeferredXMLObject(Context context, XMLCodingScheme codingScheme, ObjectType type,XMLNode node) {
		this.type=type;
		this.context=context;
		this.scheme=codingScheme;
		this.node=node;
	}
	
	@Override
	public <T> T get(Class<T> clazz) throws BaseException {
		return get(context,clazz);
	}

	@Override
	public ObjectType getObjectType(Namespace ns) {
		if(node!=null) {
			return type==null?null:ns.equals(type.getNamespace())?type:null;
		} else {
			return super.getObjectType(ns);
		}
	}

	@Override
	protected Class<?>[] getProxyClasses(Class<?> clazz) {
		return new Class[] { clazz, XMLNodeHolder.class, Deferred.class, Castable.class };
	}
	
	public void setObject(Object o) {
		super.setObject(o);
		node=null;
		context=null;
		scheme=null;
	}

	@Override
	public <T> T get(Context context,Class<T> clazz) throws BaseException {
		T t=super.get(clazz);
		if(t==null&&this.node!=null) try {
			TypeDefinition def=type!=null?context.resolveType(type):context.resolveType(clazz);
			Class<?> typeClass=def.getJavaClassType()==null?null:def.getJavaClassType().asLinkedLocal(context.getTypeLoader().getClassLoader()).asClass();
			if(typeClass!=null&&clazz.isAssignableFrom(typeClass)) {
				t=scheme.newBuilder().setRootTag(this.node.name).setRootType(def).build().createDecoder(context, this.node).decode(clazz);
			}
			if(t!=null&&context==this.context) {
				setObject(t);
			}
		} catch(Throwable e) {
			return BaseException.throwException(e);
		}
		return t;
	}

	
	@Override
	public boolean isResolved() {
		return node==null;
	}

	public static class XMLNode implements Attributes {
		String name;
		String[] attributes;
		String text="";
		List<XMLNode> children=new ArrayList<DeferredXMLObject.XMLNode>();
		public XMLNode(String name,Attributes attributes) {
			this.name=name;
			int n=attributes.getLength();
			this.attributes=new String[2*n];
			for(int i=0;i<n;i++) {
				this.attributes[2*i]=attributes.getQName(i);
				this.attributes[2*i+1]=attributes.getValue(i);
			}
		}
		public void add(char[] ch, int start, int length) {
			if(children.size()==0) {
				text+=new String(ch,start,length);
			}
		}
				
				@Override
				public String getValue(String uri, String localName) {
					return null;
				}
				
				@Override
				public String getValue(String qName) {
					return getValue(getIndex(qName));
				}
				
				@Override
				public String getValue(int index) {
					return index>=0?attributes[2*index+1]:null;
				}
				
				@Override
				public String getURI(int index) {
					return null;
				}
				
				@Override
				public String getType(String uri, String localName) {
					return null;
				}
				
				@Override
				public String getType(String qName) {
					return null;
				}
				
				@Override
				public String getType(int index) {
					return null;
				}
				
				@Override
				public String getQName(int index) {
					return index>=0?attributes[2*index]:null;
				}
				
				@Override
				public String getLocalName(int index) {
					return index>=0?attributes[2*index]:null;
				}
				
				@Override
				public int getLength() {
					return attributes.length>>1;
				}
				
				@Override
				public int getIndex(String uri, String localName) {
					return 0;
				}
				
				@Override
				public int getIndex(String qName) {
					for(int i=0;i<attributes.length;i+=2) {
						if(attributes[2*i].equals(qName)) {
							return i>>1;
						}
					}
					return -1;
				}
		
		public void add(XMLNode node) {
			children.add(node);
			text="";
		}
	}

	public XMLNode getNode() {
		return node;
	}

}
