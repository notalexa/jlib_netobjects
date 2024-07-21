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
package not.alexa.netobjects.protobuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.Codecs;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ArrayTypeDefinition.ArrayFlavour;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder.FieldBuilder;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.TypeResolver;
import not.alexa.netobjects.types.access.AbstractAccessibleObject;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.ArrayTypeAccess;
import not.alexa.netobjects.types.access.DefaultAccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessFactory.AccessResolver;
import not.alexa.netobjects.types.access.MapEntryAccess;

/**
 * Protobuf type resolver. The resolver checks if the class is a protobuf class
 * and generates a type definition out of the message description. Additionally,
 * for this classes access is generated using the getter and setter methods of the
 * class.
 * 
 * @author notalexa
 */
public class ProtobufResolver implements TypeResolver {
	Codecs codecs;
	static {
		DefaultAccessFactory.addAccessResolver(new ProtobufAccessResolver());
	}

    public ProtobufResolver() {
    	codecs=Codecs.defaultTextCodecs();
    }
    
    private static Class<?> normalizeClass(Class<?> clazz) {
    	if(Collection.class.isAssignableFrom(clazz)) {
    		return Iterable.class;
    	} else {
    		return clazz;
    	}
    }
    
    private TypeDefinition getPrimitiveTypeDefinition(FieldDescriptor.Type type) {
    	switch(type) {
	    	case BOOL: return PrimitiveTypeDefinition.getTypeDescription(Boolean.class);
	    	case BYTES: return PrimitiveTypeDefinition.getTypeDescription(byte[].class);
	    	case DOUBLE: return PrimitiveTypeDefinition.getTypeDescription(Double.class);
	    	case FLOAT: return PrimitiveTypeDefinition.getTypeDescription(Float.class);
	    	case FIXED32:
	    	case SFIXED32:
	    	case INT32:
	    	case SINT32:
	    	case UINT32: return PrimitiveTypeDefinition.getTypeDescription(Integer.class);
	    	case FIXED64:
	    	case SFIXED64:
	    	case INT64:
	    	case SINT64:
	    	case UINT64: return PrimitiveTypeDefinition.getTypeDescription(Long.class);
	    	case STRING: return PrimitiveTypeDefinition.getTypeDescription(String.class);
	    	case ENUM:
	    	case MESSAGE:
	    	case GROUP: 
	    	default: return null;
    	}
    }
    
    private void addHints(FieldDescriptor.Type type,FieldBuilder builder) {
    	switch(type) {
	    	case UINT32:
	    	case UINT64:builder.addHint("protobuf:unsigned");
	    		break;
	    	case SFIXED32:
	    	case SFIXED64:
	    	case SINT32:
	    	case SINT64:builder.addHint("protobuf:signed");
	    	default:
	    		break;
		}
    	switch(type) {
	    	case FIXED32:
	    	case FIXED64:
	    	case SFIXED32:
	    	case SFIXED64:builder.addHint("protobuf:fixed");
	    	default:
	    		break;
    	}
    }
    
    private static FieldAccessor getFieldAccessor(Class<?> clazz,FieldDescriptor descriptor) throws Throwable {
    	Class<?> builderClass=clazz.getMethod("newBuilder").getReturnType();
    	String methodName=descriptor.getName();
    	methodName=Character.toUpperCase(methodName.charAt(0))+methodName.substring(1);
    	int p;
    	while((p=methodName.indexOf('_'))>0) {
    		methodName=methodName.substring(0,p)+Character.toUpperCase(methodName.charAt(p+1))+methodName.substring(p+2);
    	}
    	String getterName=methodName;
    	String setterName=methodName;
    	Class<?> rootClass=null;
    	if(descriptor.isRepeated()) {
    		if(descriptor.isMapField()) {
    			getterName="get"+getterName+"Map";
    			setterName="putAll"+setterName;
    		} else {
    			getterName="get"+getterName+"List";
    			setterName="addAll"+setterName;
    			try {
    				rootClass=clazz.getMethod("get"+methodName,Integer.TYPE).getReturnType();
    			} catch(Throwable t) {
    			}
    		}
    	} else {
    		getterName="get"+getterName;
    		setterName="set"+setterName;
    	}
    	java.lang.reflect.Method getter=clazz.getMethod(getterName);
    	java.lang.reflect.Method setter=builderClass.getMethod(setterName,normalizeClass(getter.getReturnType()));
    	return new FieldAccessor(rootClass,getter, setter);
    }
    
    private TypeDefinition resolveMessageType(LoaderIntermediate loader, Class<?> clazz,ObjectType type,Descriptor descriptor) {
		ClassTypeDefinition.Builder classType=new ClassTypeDefinition(type).createBuilder();
		List<FieldDescriptor> fields=new ArrayList<>(descriptor.getFields());
		Collections.sort(fields, new Comparator<FieldDescriptor>() {
			@Override
			public int compare(FieldDescriptor arg0, FieldDescriptor arg1) {
				return arg0.getIndex()-arg1.getIndex();
			}
		});
		for(FieldDescriptor field:fields) try {
			FieldAccessor accessor=null;
			if(clazz!=null) {
				accessor=getFieldAccessor(clazz, field);
			}
			FieldDescriptor.Type protobufFieldType=field.getType();
			TypeDefinition fieldType=null;
			switch(protobufFieldType) {
    			case GROUP:  throw new BaseException(BaseException.BAD_REQUEST,"Not implemented: "+fieldType);
    			case ENUM: fieldType=loader.resolveType(ObjectType.createClassType(accessor.getRootClass()));// new BaseException(BaseException.BAD_REQUEST,"Not implemented: "+fieldType);
    				break;
    			case MESSAGE:
    				Class<?> messageClass=accessor==null?null:accessor.getRootClass();
    		    	if(messageClass!=null) {
    		    		fieldType=loader.resolveType(ObjectType.createClassType(messageClass));
    		    	} else {
    		    		fieldType=resolveMessageType(loader,messageClass,messageClass==null?null:ObjectType.createClassType(messageClass),field.getMessageType());
    		    	}
    				break;
    			default: fieldType=getPrimitiveTypeDefinition(protobufFieldType);
    				break;
			}
			if(fieldType!=null) {
				Object defaultValue=null;
				if(!field.hasPresence()) try {
					defaultValue=field.getDefaultValue();
					if(defaultValue instanceof ByteString) {
						defaultValue=((ByteString)defaultValue).toByteArray();
					} else if(defaultValue instanceof EnumValueDescriptor) {
						defaultValue=((EnumValueDescriptor)defaultValue).getName();//   accessor.getRootClass().getMethod("valueOf",String.class).invoke(null, ((EnumValueDescriptor)defaultValue).getName());
					}
				} catch(Throwable t) {
					defaultValue=null;
				}
				if(field.isRepeated()) {
					fieldType=new ArrayTypeDefinition(field.isMapField()?ArrayFlavour.Map:ArrayFlavour.Array,fieldType);
					defaultValue="empty";
				}
				FieldBuilder fieldBuilder=classType.createField(field.getName(), fieldType)
					.setOptional(field.isOptional())
				    .setNumber(field.getNumber())
				    .setDefaultValue(defaultValue);
				if(!field.getName().equals(field.getJsonName())) {
					fieldBuilder.addTag("json",field.getJsonName());
				}
				addHints(protobufFieldType, fieldBuilder);
				fieldBuilder.build();
			} else {
				System.out.println("Unsupported.");
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
    	return classType.build();
    }

	@Override
	public TypeDefinition resolve(LoaderIntermediate loader, ObjectType type) {
		if(type instanceof Type) try {
            Type t=(Type)type;
            LinkedLocal linkedClass=t.asLinkedLocal(loader.getClassLoader());
            Class<?> clazz=linkedClass.asClass();
            if(GeneratedMessageV3.class.isAssignableFrom(clazz)) {
        		Descriptor descriptor=(Descriptor)clazz.getMethod("getDescriptor").invoke(null);
        		return resolveMessageType(loader,clazz,type, descriptor);
            }
        } catch(Throwable t) {
        	t.printStackTrace();
        }
		return null;
	}
	
	private static class ProtobufAccessResolver implements AccessResolver {

		@Override
		public Access resolve(DefaultAccessFactory factory, ClassLoader currentClassLoader, TypeDefinition type) {
			Class<?> clazz=type.getJavaClassType().asLinkedLocal(currentClassLoader).asClass();
			if(clazz!=null&&GeneratedMessageV3.class.isAssignableFrom(clazz)) {
				return new ProtobufAccess(factory,type,clazz);
			}
			return null;
		}
		
	}

	static class ProtobufAccess implements Access {
		DefaultAccessFactory factory;
		TypeDefinition type;
		Class<?> clazz;
		Map<String,RuntimeInfo> descriptors=new HashMap<String, RuntimeInfo>();
		
		public ProtobufAccess(DefaultAccessFactory factory, TypeDefinition type,Class<?> clazz) {
			this.factory = factory;
			this.type = type;
			this.clazz=clazz;
			try {
        		Descriptor descriptor=(Descriptor)clazz.getMethod("getDescriptor").invoke(null);
        		for(FieldDescriptor field:descriptor.getFields()) {
        			FieldAccessor accessor=getFieldAccessor(clazz, field);
        			descriptors.put(field.getName(), new RuntimeInfo(field,accessor));
        		}
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}

		@Override
		public AccessFactory getFactory() {
			return factory;
		}

		@Override
		public TypeDefinition getType() {
			return type;
		}
		
		private Access resolveAccess(FieldDescriptor descriptor,TypeDefinition type) {
			if(descriptor.isRepeated()) {
				ArrayTypeDefinition arrayType=(ArrayTypeDefinition)type;
				if(descriptor.isMapField()) {
					return resolveMapAccess(descriptor.getMessageType(), arrayType);
				} else {
					return new ArrayTypeAccess(arrayType,factory.resolve(this,arrayType.getComponentType()), List.class);
				}
			} else {
				return factory.resolve(this, type);
			}
		}
		
		private Access resolveMapAccess(Descriptor descriptor,ArrayTypeDefinition type) {
			ClassTypeDefinition entry=(ClassTypeDefinition)type.getComponentType();
			Access keyAccess=resolveAccess(descriptor.getFields().get(0),entry.getFields()[0].getType());
			Access valueAccess=resolveAccess(descriptor.getFields().get(1),entry.getFields()[1].getType());
			return new ArrayTypeAccess(type,
					new MapEntryAccess(factory, (ClassTypeDefinition)type.getComponentType(), new Access[] {
							keyAccess,
							valueAccess,
					}), Map.class);
		}

		@Override
		public Access getFieldAccess(Field f) throws BaseException {
			FieldDescriptor descriptor=descriptors.get(f.getName()).fieldDescriptor;
			return resolveAccess(descriptor, f.getType());
		}

		@Override
		public Object getField(Object o, Field f) throws BaseException {
			RuntimeInfo info=descriptors.get(f.getName());
			if(info!=null) try {
				return info.accessor.get(o);
			} catch(Throwable t) {
				return BaseException.throwException(t);
			}
			return null;
		}


		@Override
		public void setField(Object o, Field f, Object v) throws BaseException {
			RuntimeInfo info=descriptors.get(f.getName());
			if(info!=null) try {
				info.accessor.put(o,v);
			} catch(Throwable t) {
				BaseException.throwException(t);
			}
		}

		@Override
		public AccessibleObject newAccessible(AccessContext context) throws BaseException {
			try {
				return new AbstractAccessibleObject(this) {
					com.google.protobuf.Message.Builder builder=(com.google.protobuf.Message.Builder)clazz.getMethod("newBuilder").invoke(null);
					Object o;
					@Override
					public Object getObject() {
						return builder;
					}
					
					@Override
					public void setField(Field f, AccessibleObject v) throws BaseException {
						o=null;
						super.setField(f, v);
					}

					@Override
					public synchronized Object getAssignable() throws BaseException {
						if(o==null) {
							o=builder.build();
						}
						return o;
					}
				};
			} catch(Throwable t) {
				return BaseException.throwException(t);
			}
		}
	}
	
	static class FieldAccessor {
		// Called on object
		java.lang.reflect.Method getter;
		// Called on builder
		java.lang.reflect.Method setter;
		
		Class<?> rootClass;
		int modifier;
		private FieldAccessor(Class<?> rootClass,java.lang.reflect.Method getter,java.lang.reflect.Method setter) {
			this.getter=getter;
			this.rootClass=rootClass;
			if(rootClass==null&&!Map.class.isAssignableFrom(getter.getReturnType())) {
				this.rootClass=getter.getReturnType();
			}
			if(ByteString.class.equals(getter.getReturnType())) {
				modifier=1;
			}
			this.setter=setter;
		}
		
		public Class<?> getRootClass() {
			return rootClass;
		}
		
		private Object get(Object o) throws Throwable {
			Object val=getter.invoke(o);
			if(val!=null) {
				switch(modifier) {
					case 1:return ((ByteString)val).toByteArray();
				}
			}
			return val;
		}
		
		private void put(Object o,Object v) throws Throwable {
			switch(modifier) {
				case 1:v=ByteString.copyFrom((byte[])v);
					break;
			}
			setter.invoke(o,v);
		}
	}
	
	private static class RuntimeInfo {
		FieldDescriptor fieldDescriptor;
		FieldAccessor accessor;
		public RuntimeInfo(FieldDescriptor fieldDescriptor, FieldAccessor accessor) {
			this.fieldDescriptor=fieldDescriptor;
			this.accessor=accessor;
		}
	}
}
