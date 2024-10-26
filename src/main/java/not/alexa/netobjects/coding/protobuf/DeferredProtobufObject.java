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
package not.alexa.netobjects.coding.protobuf;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Castable;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.BufferWriter;
import not.alexa.netobjects.types.Deferred;
import not.alexa.netobjects.types.DeferredObject;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;

/**
 * Representing a deferred object from a protobuf source.
 * 
 * @author notalexa
 */
class DeferredProtobufObject extends DeferredObject implements BufferWriter {
	Context context;
	ProtobufData content;
	ProtobufCodingScheme scheme;
	ObjectType type;
	
	public DeferredProtobufObject() {
	}

	public DeferredProtobufObject(Object o) {
		super(o);
	}

	public DeferredProtobufObject(ProtobufCodingScheme scheme,Context context,long v) {
		this.context=context;
		this.scheme=scheme;
		this.content=new ProtobufData(v);
	}
	
	public DeferredProtobufObject(ProtobufCodingScheme scheme,Context context,ObjectType type,byte[] content) {
		this.context=context;
		this.type=type;
		this.scheme=scheme;
		this.content=new ProtobufData(content);
	}
	
	@Override
	public <T> T get(Class<T> clazz) throws BaseException {
		return get(context,clazz);
	}
	
	public void write(ProtobufBuffer buffer,int index) {
		content.write(index, buffer);
	}

	@Override
	protected Class<?>[] getProxyClasses(Class<?> clazz) {
		return new Class[] { clazz, BufferWriter.class, Deferred.class, Castable.class };
	}
	
	@Override
	public ObjectType getObjectType(Namespace ns) {
		if(content!=null) {
			return type==null?null:ns.equals(type.getNamespace())?type:null;
		} else {
			return super.getObjectType(ns);
		}
	}

	public void setObject(Object o) {
		super.setObject(o);
		content=null;
		context=null;
		scheme=null;
	}

	@Override
	public <T> T get(Context context,Class<T> clazz) throws BaseException {
		T t=super.get(clazz);
		if(t==null&&this.content!=null) try {
			TypeDefinition def=type!=null?context.resolveType(type):context.resolveType(clazz);
			Class<?> typeClass=def.getJavaClassType()==null?null:def.getJavaClassType().asLinkedLocal(context.getTypeLoader().getClassLoader()).asClass();
			if(typeClass!=null&&clazz.isAssignableFrom(typeClass)) {
				t=scheme.newBuilder().setRootType(def).build().createDecoder(context, content.getOutputBuffer(clazz)).decode(clazz);
			}
			if(t!=null&&context==this.context) {
				setObject(t);
			}
		} catch(Throwable e) {
			return BaseException.throwException(e);
		}
		return t;
	}

	
	public boolean isResolved() {
		return content==null;
	}

	private class ProtobufData {
		long v;
		byte[] b;
		public ProtobufData(long v) {
			this.v=v;
		}
		public ProtobufData(byte[] b) {
			this.b=b;
		}
		
		private ProtobufBuffer getOutputBuffer(Class<?> clazz) {
			if(scheme!=null&&scheme.getPrimitiveTypeCodec(clazz)!=null) {
				return (b!=null?new ProtobufBuffer().write(1, b):new ProtobufBuffer().write(1, v)).getOutputBuffer();
			} else {
				return new ProtobufBuffer(b);
			}
		}
		
		void write(int index,ProtobufBuffer buffer) {
			if(b!=null) {
				buffer.write(index,b);
			} else {
				buffer.write(index, v);
			}
		}
	}
}
