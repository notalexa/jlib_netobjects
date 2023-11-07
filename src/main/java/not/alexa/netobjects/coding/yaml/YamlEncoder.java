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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.AbstractTextCodingScheme.TextCodingItem;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.TextCodingSupport;
import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.OutputHandler;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme.YAMLCodingExtraInfo;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.ArrayTypeAccess;

/**
 * Encoder implementation for YAML.
 * 
 * @author notalexa
 *
 */
class YamlEncoder extends TextCodingItem<YamlCodingScheme,YamlEncoder> implements Encoder,Encoder.Buffer {
    static final int SHOWTYPE_MASK=4;
	protected OutputHandler writer;
	private int flags;
	protected Codec codec;
	protected Runnable typeWriter;
	
	YamlEncoder(TextCodingSupport<YamlCodingScheme> root,OutputHandler handler) {
		super(root);
		writer=handler;
	}
	
	private YamlEncoder(YamlEncoder parent) {
		super(parent);
		this.writer=parent.writer;
	}
	
	@Override
	public void write(byte[] encoded) throws BaseException {
		throw new BaseException();
	}
	
	protected void writeField(String name,String value) throws BaseException {
		try {
			writer.scalar(true,name);
			writer.scalar(false, value);
		} catch(IOException e) {
			BaseException.throwException(e);
		}
	}

	private boolean showName() {
		return (flags&1)==0;
	}
    private boolean showType() {
        return (flags&SHOWTYPE_MASK)!=0;
    }

	@Override
	public Encoder encode(Object o) throws BaseException {
	    if(o!=null) try {
	    	if(parent!=null) {
	    		if(showName()) {
	    			writer.scalar(true, fieldName);
	    		}
	    	} else {
	    		writer.beginDocument();
	    	}
    		if(getType().getFlavour()==Flavour.ArrayType) {
    			Collection<?> col=ArrayTypeAccess.canonicalize(o);
    			typeWriter=null;
    			writer.beginArray(false,Collections.emptyList());
    			for(Object c:col) {
    				encode0(c);
    			}
    			writer.endArray(false);
    		} else {
    			encode0(o);
    		}
    		if(parent==null) {
    			writer.endDocument();
    		}
    	} catch(IOException e) {
    		BaseException.throwException(e);
	    }
		return this;
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
    	    boolean needsDot=false;
    	    boolean jsonPrimitiv=false;
    	    switch(type.getFlavour()) {
	    	    case PrimitiveType:
	    	    case EnumType: jsonPrimitiv=!showType;
	    	    case ArrayType: needsDot=showType;
	    	    	break;
    	    	default:
    	    		break;
    	    }
    	    if(jsonPrimitiv) {
    	    	codec.encode(this,o);
    	    } else {
    	    	TypeDefinition objectDefinition=type;
    	    	AtomicBoolean closeUp=new AtomicBoolean(false);
    	    	typeWriter=()->{
    	    		try {
    	    			closeUp.set(true);
    	    			writer.beginObject(false,Collections.emptyList());
		    	    	if(showType) {
		    				writeField(root.getCodingScheme().getTypeRef(),objectDefinition.getType(root.getCodingScheme().getNamespace()).getName());
		    	    	}
	    	    	} catch(Throwable t) {
	    	    	} finally {
	    	    		typeWriter=null;
	    	    	}
    	    	};
    	    	if(needsDot) {
    	    		typeWriter.run();
    	    		writer.scalar(true, ".");
        	    	codec.encode(this,o);
    	    	} else {
    	    		codec.encode(this, o);
    	    	}
    	    	if(closeUp.get()) {
    	    		writer.endObject(false);
    	    	}
    	    }
		} catch(Throwable t) {
			BaseException.throwException(t);
		} finally {
		    this.codec=stackedCodec;
            this.access=stackedAccess;
		}
	}
	
	@Override
	public void write(CharSequence encoded) throws BaseException {
		try {
			writer.scalar(false,encoded.toString());
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

	public YamlEncoder init(String fieldName,int flags,Access access) {
		super.init(fieldName, access);
		this.flags=flags;
		this.codec=null;
		return this;
	}

	@Override
	protected YamlEncoder createChild() {
		return new YamlEncoder(this);
	}

	@Override
	public Encoder push(Object ctx,Field f) throws BaseException {
	    if(ctx instanceof YAMLCodingExtraInfo) {
	        YAMLCodingExtraInfo info=(YAMLCodingExtraInfo)ctx;
	        YamlEncoder c=getChild().init(info.getName(f),info.getFlags(f),access.getFieldAccess(f));
	        if(codec!=null) {
	            c.codec=codec.getCodec(f);
	        }
	        return c;
	    } else {
	        throw new BaseException(BaseException.BAD_REQUEST,"Illegal push: context must be of type "+YAMLCodingExtraInfo.class.getName());
	    }
	}

	@Override
	public boolean isReferenced(Object o) throws BaseException {
	    if(access.getType().getFlavour()==Flavour.ClassType&&((ClassTypeDefinition)access.getType()).enableObjectRefs()) try {
	    	Object ref=root.getObjectReference(o);
		    if(ref!=null) {
		    	typeWriter=null;
		    	writer.scalar(false,Collections.emptyList(),new Token.SimpleToken(Type.Alias, null,ref.toString()));
				return true;
			} else if(parent!=null) {
				ref=root.getObjectReference(o);
				if(ref!=null) {
				   	//writer.append("&").append(ref.toString()).append(root.getCodingScheme().getLineTerminator());
				}
			}
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
		if(typeWriter!=null) {
			typeWriter.run();
		}
	    return false;
	}
}
