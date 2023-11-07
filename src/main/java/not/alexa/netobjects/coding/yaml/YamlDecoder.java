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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.AbstractTextCodingScheme.TextCodingItem;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.TextCodingSupport;
import not.alexa.netobjects.coding.yaml.Token.DecoratedToken;
import not.alexa.netobjects.coding.yaml.Yaml.Document;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme.YAMLCodingExtraInfo;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.Constructor;

/**
 * Decoder implementation for YAML.
 * 
 * @author notalexa
 *
 */
class YamlDecoder implements Decoder {
	private InputStream stream;
	private TextCodingSupport<YamlCodingScheme> root;
	private Iterator<Document> documents;

	YamlDecoder(TextCodingSupport<YamlCodingScheme> root,Document doc) {
		this.root=root;
		this.documents=Collections.singleton(doc).iterator();
	}

	YamlDecoder(TextCodingSupport<YamlCodingScheme> root,InputStream stream) {
	    this.root=root;
		this.stream=stream;
		documents=root.getCodingScheme().yaml.parse(stream).iterator();
	}

	@Override
	public <T> T decode(Class<T> clazz) throws BaseException {
		try {
			YAMLContentHandler top=new YAMLContentHandler(this, root.getContext())
					.init("",root.getCodingScheme().getRootDecoder(root));
			if(documents.hasNext()) {
				documents.next().process(new Yaml.DefaultHandler(false,top::decode));
				return root.getContext().cast(clazz,top.getResult());
			}
			return null;
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
	public boolean hasNext() {
		return documents.hasNext();
	}
	
	private static class YAMLContentHandler extends TextCodingItem<YamlCodingScheme,YAMLContentHandler> implements Decoder.Buffer {
	    protected String content=null;
	    protected Field field;
	    protected AccessibleObject current=null;

	    public YAMLContentHandler(YamlDecoder decoder, Context context) {
	        super(decoder.root);
	    }
	    
	    public void decode(Token t,Throwable e) {
	    	if(e!=null) {
	    		current=new AccessibleObject.Adapter() {
					
					@Override
					public TypeDefinition getType() {
						return null;
					}
					
					@Override
					public Object getObject() {
						return e;
					}
					@Override
					public Object getAssignable() throws BaseException {
						return BaseException.throwException(e);
					}
				};
	    	} else try {
	    		current=decode(t);
	    	} catch(BaseException e0) {
	    		current=new AccessibleObject.Adapter() {
					
					@Override
					public TypeDefinition getType() {
						return null;
					}
					
					@Override
					public Object getObject() {
						return e0;
					}
					@Override
					public Object getAssignable() throws BaseException {
						throw e0;
					}
				};
	    	}
	    	
	    }
	    
	    private Object getResult() throws BaseException {
	    	return current.getAssignable();
	    }
	    
	    public AccessibleObject decode(Token e) throws BaseException {
	    	List<String> modifiers=Collections.emptyList();
	    	if(e.getType()==not.alexa.netobjects.coding.yaml.Token.Type.DecoratedToken) {
	    		modifiers=new ArrayList<>();
		    	while(e.getType()==not.alexa.netobjects.coding.yaml.Token.Type.DecoratedToken) {
		    		DecoratedToken t=(DecoratedToken)e;
		    		if(t.getDecorator().getType()==not.alexa.netobjects.coding.yaml.Token.Type.Anchor) {
		    			modifiers.add(t.getDecorator().getValue());
		    		}
		    		e=t.getToken();
		    	}
	    	}
	    	if(e.getType()==not.alexa.netobjects.coding.yaml.Token.Type.Alias) {
        		AccessibleObject v=root.resolveObjectReference(e.getValue());
        		if(v!=null) {
                	for(String modifier:modifiers) {
            			root.addObjectReference(false, modifier, v);
                	}
        			return v;
        		} else {
    				throw new BaseException(BaseException.NOT_FOUND,"Undefined reference: "+e.getValue());
        		}
	    	}
	    	Map<String,Token> o=null;
	        Access tagAccess=access;
	        TypeDefinition fieldType=tagAccess.getType();
	    	if(tagAccess.getType().isAbstract()) {
	    		if(e.getType()==not.alexa.netobjects.coding.yaml.Token.Type.Map) try {
	    			o=e.getMap();
                    Token clazz=o.get(root.getCodingScheme().getTypeRef());
                    if(clazz!=null&&clazz.getType()==not.alexa.netobjects.coding.yaml.Token.Type.Scalar) try {
                        fieldType=getContext().getTypeLoader().resolveType(getCodingScheme().getNamespace().create(clazz.getValue()));
                        tagAccess=root.getFactory().resolve(getContext(),fieldType);
                    } catch(Throwable t) {
                        return BaseException.throwException(t);
                    }
                    switch(fieldType.getFlavour()) {
	    	    	    case PrimitiveType:
	    	    	    case EnumType:
	    	    	    case ArrayType:e=o.get(".");
	    	    	    	break;
	   	    	    	default:
	   	    	    		break;
    	    	    }
	    		} catch(YamlException ex) {
	    			return BaseException.throwException(ex);
	    		}
	    	}
	    	switch(fieldType.getFlavour()) {
	    	    case ClassType:
	    	    	ClassTypeDefinition classType=(ClassTypeDefinition)fieldType;
	                boolean objRefs=classType.enableObjectRefs();
	                YAMLCodingExtraInfo extraInfo=YamlCodingScheme.getExtraInfo(classType);
	                if(e.getType()==not.alexa.netobjects.coding.yaml.Token.Type.Map) try {
	                	o=e.getMap();
                		current=tagAccess.newAccessible(this);
	                	if(objRefs) {
                			root.addObjectReference(objRefs, null, current);
	                	}
	                	if(modifiers.size()>0) for(String modifier:modifiers) {
                			root.addObjectReference(false, modifier, current);
	                	}
	                	for(Field f:classType.getFields()) {
	                		String name=extraInfo.getName(f);
	                		Token field=o.get(name);
	                		if(field!=null) {
	                			AccessibleObject v=getChild().init(name,tagAccess.getFieldAccess(f)).decode(field);
	                			current.setField(f, v);
	                		}
	                	}
	                	return current;
	                } catch(YamlException ex) {
	                	return BaseException.throwException(ex);
	                }
	                break;
	    	    case ArrayType:
	    	    	List<Token> a=e.getArray();
	    	    	AccessibleObject array=tagAccess.newAccessible(this);
	    	    	if(modifiers.size()>0) for(String modifier:modifiers) {
            			root.addObjectReference(false, modifier, array);
                	}
	    	    	Access componentAccess=tagAccess.getComponentAccess();
	    	    	for(Token c:a) {
	    	    		array.add(getChild().init(fieldName,componentAccess).decode(c));
	    	    	}
	    	    	return array;
	    	    case PrimitiveType:
	    	    case EnumType://Access simpleAccess=new Access.SimpleTypeAccess(getCodingScheme().getFactory(),fieldType);
	            	Codec codec=resolveCodec(fieldType.getJavaClassType(),tagAccess);
	            	content=e.getValue();
	            	AccessibleObject simpleType=tagAccess.makeAccessible(codec.decode(this));
	            	if(modifiers.size()>0) for(String modifier:modifiers) {
            			root.addObjectReference(false, modifier, simpleType);
                	}
	            	return simpleType;
	    	    default:
	    	    	break;
	    	}
	    	return null;
	    }

	    public YAMLContentHandler(YAMLContentHandler parent) {
	        super(parent);
	    }

	    @Override
	    protected YAMLContentHandler createChild() {
	        return new YAMLContentHandler(this);
	    }

	    @Override
	    public YAMLContentHandler init(String fieldName, Access access) {
	        super.init(fieldName,access);
	        content=null;
	        current=null;
	        field=null;
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
	    
	    @Override
	    public <T> T castTo(Context context, Class<T> clazz) {
	    	if(current!=null) {
	            T t=current.castTo(context, clazz);
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
