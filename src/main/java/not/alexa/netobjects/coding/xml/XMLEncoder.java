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
import not.alexa.netobjects.coding.xml.XMLCodingScheme.XMLCodingExtraInfo;
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
    static final int SHOWTYPE_MASK=4;
	protected String indent;
	protected Writer writer;
	private boolean closed;
	private boolean hasChildren;
	protected int flags;
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
	
	private boolean isAttribute() {
	    return (flags&3)==1;
	}
	
    private boolean isTag() {
        return (flags&3)==0;
    }

    private boolean showType() {
        return (flags&SHOWTYPE_MASK)!=0;
    }

	@Override
	public Encoder encode(Object o) throws BaseException {
	    if(o!=null) try {
	    	boolean root=parent==null;
	    	if(root) {
	    		getCodingScheme().writeHeader(writer);
	    	}
    		if(getType().getFlavour()==Flavour.ArrayType) {
    			Collection<?> col=ArrayTypeAccess.canonicalize(o);
    			if(col.size()==0) {
    	            if(parent!=null) {
    	                parent.closeOpener();
    	                writer.append(indent);
    	            }
    			    writer.append('<').append(fieldName.toString()).append(' ').append(getCodingScheme().getReservedAttributes().getIsEmptyName()).append("=\"true\"/>");
    			} else if(!root) {
    				for(Object c:col) {
    					encode0(c);
    				}
    			} else {
    				writer.append('<').append(fieldName.toString());
    				XMLEncoder level=getChild().init(fieldName.toString(), flags, access);
    				level.codec=codec;
    				level.encode(o);
    				if(closed) {
    					writer.append(indent).append("</").append(fieldName.toString()).append('>');
    				} else {
    					writer.append("/>");
    				}
    			}
    		} else {
    			encode0(o);
    		}
    		if(root) {
    			writer.append(indent);
    		}
	    } catch(IOException e) {
	    	return BaseException.throwException(e);
	    }
		return this;
	}

	protected void writeClassOrRefAttribute() throws IOException {
		if(classOrRefAttribute!=null) {
		    classOrRefAttribute.write();
		    classOrRefAttribute=null;
		}
	}
	
	protected void encode0(Object o) throws BaseException, IOException {
        Codec stackedCodec=codec;
        Access stackedAccess=access;
        TypeDefinition type=access.getType();
        try {
            if(getType().getFlavour()==Flavour.ArrayType) {
                codec=codec.getComponentCodec();
                type=((ArrayTypeDefinition)type).getComponentType();
                access=access.getComponentAccess();
            }
            boolean showType=showType()||type.isAbstract();
    	    if(showType) {
                ObjectType objectType=ObjectType.createClassType(o.getClass());
    	        type=getContext().getTypeLoader().resolveType(objectType);
    	        access=type==null?null:root.getFactory().resolve(getContext(),type);
    	        codec=access==null?null:resolveCodec(objectType, access);
    	    }
    	    if(codec==null) {
    	        throw new BaseException(BaseException.BAD_REQUEST,"Codec unknown for "+o.getClass().getName());
    	    }
    	    switch(flags&3) {
        	    case 1:if(parent!=null) {
    					if(parent.closed) {
    						throw new BaseException(BaseException.BAD_REQUEST,"Parent tag is already closed.");
    					} else {
    						parent.writeClassOrRefAttribute();
    					}
    				}
    				writer.append(' ').append(fieldName.toString()).append("=\"");
    				codec.encode(this, o);
    				writer.append('"');
    				break;
        	    case 2:if(parent!=null) {
                        parent.closeOpener();
                    }
                    hasChildren=closed=false;
                    codec.encode(this,o);
                    parent.hasChildren=false;
                    break;
        	    case 0:if(parent!=null) {
    					parent.closeOpener();
    					writer.append(indent);
    				}
    				writer.append('<').append(fieldName.toString());
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
        				writer.append("</").append(fieldName.toString()).append('>');
        			} else {
        				writeClassOrRefAttribute();
        				writer.append("/>");
        			}
                    hasChildren=closed=false;
                    break;
			}
		} catch(Throwable t) {
			BaseException.throwException(t);
		} finally {
		    this.codec=stackedCodec;
            this.access=stackedAccess;
		}
	}
	
	protected void closeOpener() throws IOException {
		if(isTag()&&!closed) {
			closed=true;
			writeClassOrRefAttribute();
			writer.append('>');
		}
	}

	@Override
	public void write(CharSequence encoded) throws BaseException {
		try {
			closeOpener();
			writer.write(XMLHelper.encode(isAttribute(), encoded.toString()));
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

	public XMLEncoder init(String fieldName,int flags,Access access) {
		super.init(fieldName, access);
		this.codec=null;
		this.flags=flags;
		if(parent!=null) {
			parent.hasChildren|=!isAttribute();
		} else if(!access.getType().isAbstract()) try {
			this.codec=getCodingScheme().getCodec(getContext(), access.getType().getType(getCodingScheme().getNamespace()), access);
		} catch(BaseException e) {
		}
		return this;
	}

	@Override
	protected XMLEncoder createChild() {
		return new XMLEncoder(this);
	}

	@Override
	public Encoder push(Object ctx,Object name,Field f) throws BaseException {
	    if(ctx instanceof XMLCodingExtraInfo) {
	        XMLCodingExtraInfo info=(XMLCodingExtraInfo)ctx;
	        XMLEncoder c=getChild().init(info.getName(f),info.getFlags(f),access.getFieldAccess(f));
	        if(codec!=null) {
	            c.codec=codec.getCodec(f);
	        }
	        return c;
	    } else {
	        throw new BaseException(BaseException.BAD_REQUEST,"Illegal push: context must be of type "+XMLCodingExtraInfo.class.getName());
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
