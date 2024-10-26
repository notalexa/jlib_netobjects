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
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.AbstractTextCodingScheme.TextCodingItem;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.TextCodingSupport;
import not.alexa.netobjects.coding.yaml.Token.SimpleToken;
import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.OutputHandler;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme.YAMLCodingExtraInfo;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Deferred;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.ArrayTypeAccess;
import not.alexa.netobjects.types.access.RuntimeInfo;

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
	private String tag;
	protected Codec codec;
	protected Runnable typeWriter;
	protected Access nameAccess;
	
	YamlEncoder(TextCodingSupport<YamlCodingScheme> root,OutputHandler handler) {
		super(root);
		writer=handler;
	}
	
	private YamlEncoder(YamlEncoder parent) {
		super(parent);
		this.writer=parent.writer;
	}
	
	public void encode(Token t) throws BaseException {
		try {
			t.asDocument(root.getCodingScheme().yaml).process(writer);
		} catch(YamlException e) {
			BaseException.throwException(e);
		}
	}
	
	public void write(Token t) throws BaseException {
		try {
			Yaml.process0(((YamlCodingScheme)getCodingScheme()).yaml, t, writer);
		} catch(YamlException e) {
			BaseException.throwException(e);
		}
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
    	Access savedNameAccess=nameAccess;
	    if(o!=null) try {
	    	if(parent!=null) {
	    		if(showName()) {
	    			if(nameAccess==null) {
	    				writer.scalar(true, fieldName.toString());
	    			} else {
	    		        Codec stackedCodec=codec;
	    		        Access stackedAccess=access;
	    		        int savedFlags=flags;
	    		        try {
	    	                ObjectType objectType=ObjectType.createClassType(fieldName.getClass());
	    	    	        access=nameAccess;//==null?null:root.getFactory().resolve(getContext(),type);
	    	    	        nameAccess=null;
	    	    	        flags|=0x100;
	    	    	        codec=access==null||access.getType().isAbstract()?null:resolveCodec(objectType, access);
	    		        	encode0(false,true,fieldName);
	    		        } finally {
	    		        	flags=savedFlags;
	    		        	codec=stackedCodec;
	    		        	access=stackedAccess;
	    		        }
	    			}
	    		}
	    	} else {
	    		writer.beginDocument();
	    	}
    		if(getType().getFlavour()==Flavour.ArrayType) {
    			Collection<?> col=ArrayTypeAccess.canonicalize(o);
    			typeWriter=null;
    			Codec componentCodec=codec.getComponentCodec();
    			boolean inline=YamlCodingScheme.isInline(componentCodec);//(0!=(flags&0x8))&&getCodingScheme().isHintEnabled("inline");//componentCodec instanceof AccessCodec&&((AccessCodec)componentCodec).isInline();
    			if(!inline) {
    				writer.beginArray(false,Collections.emptyList());
    			} else {
    				writer.beginObject(false, Collections.emptyList());
    				nameAccess=access.getComponentAccess().getFieldAccess(access.getComponentAccess().getFields()[0]);
    			}
    			for(Object c:col) {
    				encode0(inline,false,c);
    			}
    			if(!inline) {
    				writer.endArray(false);
    			} else {
    				writer.endObject(false);
    			}
    		} else {
    			encode0(false,false,o);
    		}
    		if(parent==null) {
    			writer.endDocument();
    		}
    	} catch(IOException e) {
    		BaseException.throwException(e);
	    } finally {
	    	nameAccess=savedNameAccess;
	    }
		return this;
	}
	
	protected void encode0(boolean inline,boolean key,Object o) throws BaseException {
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
            if(o instanceof Deferred) {
            	Deferred<?,?> deferred=(Deferred<?,?>)o;
            	if(deferred.isResolved()) {
            		o=deferred.getCodingObject(this);
            		if(o==null) {
            			return;
            		}
            	} else if(o instanceof TokenHolder) {
            		Token t=((TokenHolder)o).getToken();
            		if(t!=null) {
            			write(t);
            		}
            		return;
            	} else {
        			throw new BaseException(BaseException.BAD_REQUEST, "Deferred Object of type "+o.getClass().getName());
            	}
        		access=deferred.getCodingAccess(this,root.getFactory());
    	        type=access==null?null:access.getType();//().getTypeLoader().resolveType(objectType);
    	        codec=access==null?null:resolveCodec(type.getType(getCodingScheme().getNamespace()), access);
            } else if(showType) {
    	    	Class<?> clazz=o.getClass();
    	    	ObjectType objectType;
    	    	if(Proxy.isProxyClass(clazz)&&Deferred.class.isAssignableFrom(clazz)) {
    	    		objectType=((Deferred)o).getObjectType(root.getCodingScheme().getNamespace());
    	    		if(o instanceof TokenHolder) {
            			Token t=((TokenHolder)o).getToken();
            			if(t!=null) {
            				write(t);
            			}
        				return;
    	    		}
    	    		objectType=((Deferred<?,?>)o).getObjectType(root.getCodingScheme().getNamespace());
    	    	} else {
    	    		objectType=ObjectType.createClassType(o.getClass());
    	    	}
    	        type=getContext().getTypeLoader().resolveType(objectType);
    	        access=type==null?null:root.getFactory().resolve(getContext(),type);
    	        codec=access==null?null:resolveCodec(objectType, access);
    	    }
    	    if(codec==null) {
    	        throw new BaseException(BaseException.BAD_REQUEST,"Codec unknown for "+o.getClass().getName());
    	    }
    	    boolean needsDot=false;
    	    boolean jsonPrimitiv=false;
    	    tag=null;
    	    switch(type.getFlavour()) {
	    	    case PrimitiveType: tag=root.getCodingScheme().getTag(o);
	    	    case EnumType: 
	    	    	jsonPrimitiv=!showType;
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
    	    			if(!inline) {
	    	    			closeUp.set(true);
	    	    			writer.beginObject(key,Collections.emptyList());
			    	    	if(showType) {
			    				writeField(root.getCodingScheme().getTypeRef(),objectDefinition.getType(root.getCodingScheme().getNamespace()).getName());
			    	    	}
    	    			}
	    	    	} catch(Throwable t) {
	    	    	} finally {
	    	    		typeWriter=null;
	    	    	}
    	    	};
    	    	if(needsDot) {
    	    		typeWriter.run();
    	    		flags&=~0x100;
    	    		writer.scalar(true, ".");
        	    	codec.encode(this,o);
    	    	} else {
    	    		codec.encode(this, o);
    	    	}
    	    	if(closeUp.get()) {
    	    		writer.endObject(key);
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
			if(tag!=null&&0==(flags&0x100)) {
				writer.scalar(false,Collections.emptyList(),new SimpleToken(Type.Scalar,tag,encoded.toString()));
			} else {
				writer.scalar(0!=(flags&0x100),encoded.toString());
			}
		} catch(Throwable t) {
			BaseException.throwException(t);
		}		
	}

	@Override
	public void close() throws BaseException {
		flush();
		writer=null;
	}

	@Override
	public void flush() throws BaseException {
		if(writer!=null) try {
			writer.flush();
		} catch(Throwable t) {
			BaseException.throwException(t);
		}
	}

	public YamlEncoder init(Object fieldName,int flags,Access nameAccess,Access access) {
		super.init(fieldName, access);
		this.nameAccess=nameAccess;
		if(parent==null&&!access.getType().isAbstract()) try {
			this.codec=getCodingScheme().getCodec(getContext(), access);
		} catch(BaseException e) {
			this.codec=null;
		} else {
			this.codec=null;
		}
		this.flags=flags;
		return this;
	}

	@Override
	protected YamlEncoder createChild() {
		return new YamlEncoder(this);
	}

	@Override
	public Encoder push(Object ctx,Object name,Field f) throws BaseException {
	    if(ctx instanceof YAMLCodingExtraInfo) {
	        YAMLCodingExtraInfo info=(YAMLCodingExtraInfo)ctx;
	        int flags=info.getFlags(f);
	        YamlEncoder c=getChild().init(name==null?info.getName(f):name,flags,nameAccess,access.getFieldAccess(f));
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
			}
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
		if(typeWriter!=null) {
			typeWriter.run();
		}
	    return false;
	}
	

	@Override
	public <T> T castTo(Context context, Class<T> clazz) {
		return clazz.isInstance(this)?(T)this:null;
	}

	@Override
	public RuntimeInfo resolve(Context context, not.alexa.netobjects.types.JavaClass.Type type) {
		return getCodingScheme().getFactory().resolve(context, type);
	}

	@Override
	public Access resolve(Context context, TypeDefinition type) {
		return getCodingScheme().getFactory().resolve(context, type);
	}

	@Override
	public Access resolve(Access referrer, TypeDefinition type) {
		return getCodingScheme().getFactory().resolve(referrer, type);
	}
	
	public interface TokenHolder {
		public Token getToken();
	}

}
