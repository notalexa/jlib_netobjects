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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.AbstractTextCodingScheme.TextCodingItem;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.TextCodingSupport;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.ArrayTypeAccess;

/**
 * Encoder implementation for XML.
 * 
 * @author notalexa
 *
 */
class XMLEncoder extends TextCodingItem<XMLCodingScheme,XMLEncoder> implements Encoder,Encoder.Buffer {
	protected String indent;
	protected Writer writer;
	private boolean closed;
	private boolean hasChildren;
	protected boolean attribute;
	protected DelayedWrite classOrRefAttribute;
	protected Codec codec;
	
	XMLEncoder(TextCodingSupport<XMLCodingScheme> root,OutputStream stream) {
		super(root);
		writer=new OutputStreamWriter(stream, root.getCodingScheme().getCodingCharset());
		indent=root.getCodingScheme().getLineTerminator();
	}
	
	private XMLEncoder(XMLEncoder parent) {
		super(parent);
		this.writer=parent.writer;
		indent=parent.indent+root.getCodingScheme().getIndent();
	}
	
	@Override
	public void write(byte[] encoded) throws BaseException {
		throw new BaseException();
		
	}
	
	@Override
	public Encoder encode(Object o) throws BaseException {
		if(getType().getFlavour()==Flavour.ArrayType) {
			Collection<?> col=ArrayTypeAccess.canonicalize(o);
			if(col.size()==0) try {
	            if(parent!=null) {
	                parent.closeOpener();
	                writer.append(indent);
	            }
			    writer.append('<').append(fieldName).append(' ').append(getCodingScheme().getReservedAttributes().getIsEmptyName()).append("=\"true\"/>");
			} catch(IOException e) {
			    return BaseException.throwException(e);
			} else for(Object c:col) {
				encode0(c);
			}
		} else {
			encode0(o);
		}
		return this;
		
	}

	protected void writeClassOrRefAttribute() throws IOException {
		if(classOrRefAttribute!=null) {
		    classOrRefAttribute.write();
		    classOrRefAttribute=null;
		}
	}
	
	protected void encode0(Object o) throws BaseException {
        Codec stackedCodec=codec;
        Access stackedAccess=access;
        TypeDefinition type=access.getType();
        try {
            if(getType().getFlavour()==Flavour.ArrayType) {
                codec=codec.getComponentCodec();
                type=((ArrayTypeDefinition)type).getComponentType();
                access=access.getComponentAccess();
            }
    	    boolean showType=type.getFlavour()==Flavour.InterfaceType;
    	    if(showType) {
                ObjectType objectType=ObjectType.createClassType(o.getClass());
    	        type=getContext().getTypeLoader().resolveType(objectType);
    	        access=getCodingScheme().getFactory().resolve(getContext(),type);
    	        codec=resolveCodec(objectType, access);
    	    } else if(codec==null) {
    	        throw new BaseException(BaseException.BAD_REQUEST,"Codec unknown for "+type);
    	    }
			if(attribute) {
				if(parent!=null) {
					if(parent.closed) {
						throw new BaseException(BaseException.BAD_REQUEST,"Parent tag is already closed.");
					} else {
						parent.writeClassOrRefAttribute();
					}
				}
				writer.append(' ').append(fieldName).append("=\"");
				codec.encode(this, o);
				writer.append('"');
			} else {
				if(parent!=null) {
					parent.closeOpener();
					writer.append(indent);
				}
				writer.append('<').append(fieldName);
				if(showType) {
				    TypeDefinition objectDefinition=type;
				    classOrRefAttribute=new DelayedWrite() {
						@Override
						public void write() throws IOException {
							writer.append(' ').append(root.getCodingScheme().getTypeRef()).append("=\"").append(objectDefinition.getType(root.getCodingScheme().getNamespace()).getName()).append('\"');
						}
					};
				}
				hasChildren=closed=false;
				codec.encode(this, o);
				if(closed) {
					if(hasChildren) {
						writer.append(indent);
					}
					writer.append("</").append(fieldName).append('>');
				} else {
					writeClassOrRefAttribute();
					writer.append("/>");
				}
				hasChildren=closed=false;
			}
		} catch(Throwable t) {
			BaseException.throwException(t);
		} finally {
		    this.codec=stackedCodec;
            this.access=stackedAccess;
		}
	}
	
	protected void closeOpener() throws IOException {
		if(!attribute&&!closed) {
			closed=true;
			writeClassOrRefAttribute();
			writer.append('>');
		}
	}

	@Override
	public void write(CharSequence encoded) throws BaseException {
		try {
			closeOpener();
			writer.write(XMLHelper.encode(attribute, encoded.toString()));
		} catch(Throwable t) {
			BaseException.throwException(t);
		}		
	}

	@Override
	public void close() throws BaseException {
		if(writer!=null) try {
			writer.close();
		} catch(Throwable t) {
			BaseException.throwException(t);
		} finally {
			writer=null;
		}
	}

	@Override
	public void flush() throws BaseException {
		if(writer!=null) try {
			writer.flush();
		} catch(Throwable t) {
			BaseException.throwException(t);
		}
	}

	public XMLEncoder init(String fieldName,boolean attribute,Access access) {
		super.init(fieldName, access);
		this.attribute=attribute;
		if(parent!=null) {
			parent.hasChildren|=!attribute;
		}
		this.codec=null;
		return this;
	}

	@Override
	protected XMLEncoder createChild() {
		return new XMLEncoder(this);
	}

	@Override
	public Encoder push(Field f) throws BaseException {
		//String name=f.getName();
		String name=f.getTag("XML");//getName();
		boolean attribute=name.length()>0&&name.charAt(0)=='@';
		if(attribute) {
			name=name.substring(1);
		}
		try {
    		XMLEncoder c= getChild().init(name,attribute,access.getFieldAccess(f));
            if(codec!=null) {
                c.codec=codec.getCodec(f);
            }
            return c;
		} catch(BaseException e) {
		    throw e;
		}
	}

	@Override
	public boolean isReferenced(Object o) throws BaseException {
	    Object ref=root.getObjectReference(o);
	    if(ref!=null) {
	        classOrRefAttribute=new DelayedWrite() {
				@Override
				public void write() throws IOException {
					writer.append(' ').append(root.getCodingScheme().getReservedAttributes().getObjRefName()).append("=\"").append(ref.toString()).append('"');
				}
			};
			return true;
		}
		return false;
	}
	
	public interface DelayedWrite {
	    public void write() throws IOException;
	}
}
