
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

import java.util.Arrays;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.BufferWriter;
import not.alexa.netobjects.coding.protobuf.ProtobufDecoder.ClassDefListener;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Deferred;
import not.alexa.netobjects.types.DeferredObject;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;

/**
 * Codec for an interface definition.
 * 
 * @author notalexa
 */
class AnyCodec extends ClassCodec {
	Access interfaceAccess;
	AnyCodec(Access interfaceAccess) {
		super(new Access.AbstractAccess() {
			Access objectTypeAccess=new SimpleTypeAccess(getFactory(), PrimitiveTypeDefinition.getTypeDescription(ObjectType.class));
	
	        @Override
	        public AccessFactory getFactory() {
	            return interfaceAccess.getFactory();
	        }
	        
	        @Override
	        public TypeDefinition getType() {
	            return ProtobufCodingScheme.ANY;
	        }
	
	        @Override
	        public AccessibleObject newAccessible(AccessContext context) throws BaseException {
	            return new AccessibleObject.Adapter() {
	            	AccessibleObject type;
	            	AccessibleObject delegate;
	
	            	@Override
	            	public AccessibleObject getField(AccessContext context,Field f) throws BaseException {
	            		switch(f.getIndex()) {
		            		case 0: return type;
		            		case 1: return delegate;
		            		default: return super.getField(context,f);
	            		}
	            	}
	
	            	@Override
	            	public void setField(AccessContext context,Field f,AccessibleObject value) throws BaseException {
	            		switch(f.getIndex()) {
	            			case 0: type=value;
	            				break;
	            			case 1: delegate=value;
	            				break;
	            		}
	            	}
	            	
					@Override
					public TypeDefinition getType() {
						return ProtobufCodingScheme.ANY;
					}
	
					@Override
					public Object getAssignable(AccessContext context) throws BaseException {
						return delegate.getAssignable(context);
					}
	
					@Override
					public Object getObject() {
						return delegate==null?null:delegate.getObject();
					}
				};
	        }
	
	        @Override
	        public Access getFieldAccess(Field f) throws BaseException {
	        	switch(f.getIndex()) {
		        	case 0: return objectTypeAccess;
		        	case 1:
		        	default: throw new BaseException(BaseException.NOT_FOUND, "Field #"+f.getIndex());
	        	}
	        }
	
			@Override
			public Object getField(AccessContext context,Object o, Field f) throws BaseException {
				switch(f.getIndex()) {
					case 1: return ObjectType.createClassType(o.getClass());
					default: return super.getField(context,o, f);
				}
			}
	    });
		this.interfaceAccess=interfaceAccess;
	}
	@Override
	public void encode(ProtobufEncoder encoder,ProtobufBuffer buffer, Object o) throws BaseException {
		Access access;
		ObjectType classType;
		ProtobufCodingScheme scheme=encoder.getCodingScheme();
		if(o instanceof Deferred) {
			Deferred<?,?> deferred=(Deferred<?, ?>)o;
			classType=deferred.getObjectType(scheme.getNamespace());
			if(deferred.isResolved()) {
				o=deferred.getCodingObject(encoder);
				if(o==null) {
					return;
				}
			} else if(o instanceof BufferWriter) {
				fieldCodecs[0].encode(encoder,buffer,classType);
				((BufferWriter)o).write(buffer, 2);
				return;
			} else {
				throw new BaseException();
			}
			access=deferred.getCodingAccess(encoder,scheme.getFactory());
		} else {
			classType=ObjectType.createClassType(o.getClass());
			access=scheme.getFactory().resolve(encoder.getContext(), encoder.getContext().resolveType(classType));
		}
		TypeDefinition type=encoder.getContext().resolveType(classType);
		if(classType!=null) {
			fieldCodecs[0].encode(encoder,buffer,classType);
			switch(type.getFlavour()) {
				case PrimitiveType:
					scheme.getPrimitiveTypeCodec(o.getClass()).encode(buffer,2,o);
					break;
				case EnumType:
					scheme.getEnumCodec(o.getClass()).encode(buffer,2,o);
					break;
				case ArrayType:
				case ClassType:
					byte[] content=scheme.getProtobufContent(o);
					if(content!=null) {
						buffer.write(2, content);
					} else {
						scheme.getClassCodec(/*encoder.getContext(),*/access).encode(encoder,buffer,2,o);
					}
					break;
				default: throw new BaseException(BaseException.BAD_REQUEST,"Unsupported");
			}
		} else {
			throw new BaseException(BaseException.BAD_REQUEST,"Unsupported");
		}
	}

	@Override
	public void consume(ClassDefListener listener, int field, long value) throws BaseException {
		Field[] fields=ProtobufCodingScheme.ANY.getFields();
		ObjectType type=(ObjectType)listener.currentObject().getField(listener,fields[0]).getAssignable(listener);
		TypeDefinition def=listener.getContext().resolveType(type);
		ProtobufCodingScheme scheme=listener.getCodingScheme();
		Object v;
		switch(def.getFlavour()) {
			case PrimitiveType: v=scheme.getPrimitiveTypeCodec(def.asClass(getClass().getClassLoader())).decode(value);
				break;
			case EnumType: v=scheme.getEnumCodec(def.asClass(getClass().getClassLoader())).decode(value);
				break;
			case ClassType:
				listener.currentObject().setField(listener,fields[1],listener.resolveObjectReference((int)value));
				listener.mark(1);
				return;
			default:
				throw new BaseException(BaseException.FORBIDDEN,"Unsupported Any of type "+def.getFlavour());
		}
		listener.mark(1);
		listener.currentObject().setField(listener,fields[1],new DefaultAccessibleObject(scheme.getFactory().resolve(listener.getContext(), def),v));
	}

	@Override
	public void consume(ClassDefListener listener, int field, byte[] value, int offset, int len)
			throws BaseException {
		if(field==2) {
			ProtobufCodingScheme scheme=listener.getCodingScheme();
			Field[] fields=ProtobufCodingScheme.ANY.getFields();
			ObjectType type=(ObjectType)listener.currentObject().getField(listener,fields[0]).getAssignable(listener);
			TypeDefinition def=listener.getContext().resolveType(type);
			if(def==null) {
				// Unknown implementation
				DeferredProtobufObject o=new DeferredProtobufObject(listener.getCodingScheme(),listener.getContext(), type, Arrays.copyOfRange(value, offset, offset+len));
				listener.mark(1);
				listener.currentObject().setField(listener,fields[1],scheme.getFactory().resolve(listener.getContext(), DeferredObject.getTypeDescription()).makeAccessible(listener,o.makeProxy(interfaceAccess)));
				return;
			}
			switch(def.getFlavour()) {
			case PrimitiveType:
				System.out.println(def);
				Object v=scheme.getPrimitiveTypeCodec(def.asClass(getClass().getClassLoader())).decode(value,offset,len);
				listener.mark(1);
				listener.currentObject().setField(listener,fields[1],new DefaultAccessibleObject(access.getFactory().resolve(listener.getContext(), def),v));
				break;
			case ArrayType:
			case ClassType:
				Access classAccess=access.getFactory().resolve(listener.getContext(), def);
				if(def.getFlavour()==Flavour.ArrayType) {
					classAccess=classAccess.getComponentAccess();
				}
				AccessibleObject o=scheme.getProtobufObject(listener,classAccess,value,offset,len);
				if(o!=null) {
					if(def.getFlavour()==Flavour.ArrayType) {
						listener.getArray(1, new ProtobufDecoder.Creator() {
							
							@Override
							public AccessibleObject newAccessible(AccessContext context) throws BaseException {
								return access.getFactory().resolve(listener.getContext(), def).newAccessible(context);
							}
						}).add(o);
					} else {
						listener.mark(1);
						listener.currentObject().setField(listener,fields[1],o);
					}
				} else {
					AbstractCodec codec=scheme.getClassCodec(listener.getContext(),listener.getContext().resolveType(type));
					ClassDefListener childListener=codec.createListener(listener);//.createChild(codec);
					codec.consume(value, offset, len,childListener);
					if(!codec.isArrayCodec()) {
						listener.mark(1);
						listener.currentObject().setField(listener,fields[1],childListener.finalized());
					}
				}
				break;
			default:
				break;
			}
		} else {
			super.consume(listener, field, value, offset, len);
		}
	}
}