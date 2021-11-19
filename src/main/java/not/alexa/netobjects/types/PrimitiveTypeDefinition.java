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
package not.alexa.netobjects.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.api.Final;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;

/**
 * Primitive types are the simplest types in the type hierachy. Roughly, 
 * primitive types in this context are primitive types in the java context and
 * in addition:
 * 
 * <ul>
 * <li><code>byte[]</code> since byte arrays are so important
 * <li>{@linkplain ObjectType} since the hierarchy is type safe and a type system is needed
 * <li>{@linkplain String} to allow string values.
 * <li>{@linkplain Date} to allow date values.
 * <li>{@linkplain UUID} serving as a generic id system.
 * <li>{@linkplain BigInteger} to allow integer values or arbitrary size
 * <li>{@linkplain BigDecimal} to allow numbers of arbitrary size and precision
 * <li>{@linkplain Object} is <b>not a primitive</b> type but declared as an interface
 * in this context but in the list of primitive types.
 * </ul>
 * 
 * Primitive types are important since the system expects the following convention:
 * <b>For a given coding scheme, each primitive type as a simple (character or byte array
 * based) representation.</b> Because of this, the class of primitive types cannot
 * be extended easily and consequently not by applications. Well, there is one
 * unusual case: If a new namespace is defined, the corresponding class representing
 * the type is automatically assigned to the {@link ObjectType} type. Coding schemes
 * has to consider this while initializing the base codecs.
 * 
 * Equality of primitive types can be defined by the standard relation since
 * there is one object per type but care has to be taken if a new namespace registers
 * it's types in the corresponding definitions.
 * 
 * @author notalexa
 *
 */
@Final
public final class PrimitiveTypeDefinition extends TypeDefinition {
	private static final List<TypeDefinition> PRIMITIVE_TYPES=new ArrayList<>();
	private static final Map<ObjectType,TypeDefinition> TYPE_MAP=new HashMap<>();
	static {
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(ObjectType.class));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(UUID.class));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(BigInteger.class));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(BigDecimal.class));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(String.class));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(Date.class));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(Boolean.TYPE));		
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(Character.TYPE));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(Byte.TYPE));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(Short.TYPE));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(Integer.TYPE));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(Long.TYPE));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(Float.TYPE));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(Double.TYPE));
		PRIMITIVE_TYPES.add(new PrimitiveTypeDefinition(byte[].class));
		PRIMITIVE_TYPES.add(new InterfaceTypeDefinition(Object.class));
		for(TypeDefinition description:PRIMITIVE_TYPES) {
			TYPE_MAP.put(description.getJavaClassType(),description);
		}
		TYPE_MAP.put(ObjectType.createClassType(Boolean.class),getTypeDescription(Boolean.TYPE));
		TYPE_MAP.put(ObjectType.createClassType(Character.class),getTypeDescription(Character.TYPE));
		TYPE_MAP.put(ObjectType.createClassType(Byte.class),getTypeDescription(Byte.TYPE));
		TYPE_MAP.put(ObjectType.createClassType(Short.class),getTypeDescription(Short.TYPE));
		TYPE_MAP.put(ObjectType.createClassType(Integer.class),getTypeDescription(Integer.TYPE));
		TYPE_MAP.put(ObjectType.createClassType(Long.class),getTypeDescription(Long.TYPE));
		TYPE_MAP.put(ObjectType.createClassType(Float.class),getTypeDescription(Float.TYPE));
		TYPE_MAP.put(ObjectType.createClassType(Double.class),getTypeDescription(Double.TYPE));
	}

	public static ClassTypeDefinition getTypeDescription() {
		return Types.PRIMITIVE_TYPE;
	}

	/**
	 * 
	 * @return a list of all primitive types (as mentioned in the class description,
	 * this includes the interface type associated to the object type {@link Object}).
	 */
	public static List<? extends TypeDefinition> getPrimitiveTypes() {
		return Collections.unmodifiableList(PRIMITIVE_TYPES);
	}
	
	/**
	 * Convenience method. The associated versionless object type is created for the class.
	 *  
	 * @param simpleType the type to resolve
	 * @return the type definition of the the simple type
	 */
	public static TypeDefinition getTypeDescription(Class<?> simpleType) {
		return TYPE_MAP.get(ObjectType.createClassType(simpleType));
	}
	
	/**
	 * 
	 * @param simpleType the simple type (primitive or <code>Object</code> type)
	 * @return the corresponding type definition
	 */
	public static TypeDefinition getTypeDescription(ObjectType simpleType) {
		return TYPE_MAP.get(simpleType);
	}
	private PrimitiveTypeDefinition() {
	}
	// Primitive types are fixed.
	private PrimitiveTypeDefinition(Class<?> clazz) {
		super(clazz);
	}

	/**
	 * @return the constant {@link Flavour#PrimitiveType}
	 */
	@Override
	public Flavour getFlavour() {
		return Flavour.PrimitiveType;
	}

	/**
	 * Object types are registered in the type map to ensure consistency with
	 * {@link #getTypeDescription(ObjectType)}
	 */
	@Override
	public void addTypes(ObjectType... objectTypes) {
		super.addTypes(objectTypes);
		for(ObjectType objectType:objectTypes) {
			if(objectType!=null) {
				TYPE_MAP.put(getType(objectType.getNamespace()),this);
			}
		}
	}
	
	public static class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory) {
			super(factory,getTypeDescription());
		}
		
		@Override
		public AccessibleObject newAccessible(AccessContext context) throws BaseException {
			return new DecodingInstance();
		}
		
		@Override
		public Object getField(Object o, int index) throws BaseException {
			PrimitiveTypeDefinition def=(PrimitiveTypeDefinition)o;
			switch(index) {
				case 0:return def.getTypes().toArray(new ObjectType[0]);
			}
			return null;
		}

		@Override
		public void setField(Object o, int index, Object v) throws BaseException {
			if(o instanceof DecodingInstance) {
				DecodingInstance newInstance=(DecodingInstance)o;
				switch(index) {
					case 0:newInstance.types=(ObjectType[])v;
						break;
				}
			} else {
				throw new BaseException(BaseException.BAD_REQUEST,"type is read only.");
			}
		}

		@Override
		public Access createFieldAccess(Field f) throws BaseException {
			switch(f.getIndex()) {
				case 0:return forArray(f.getType(),ObjectType[].class);
			}
			return null;
		}
		
		private class DecodingInstance extends DefaultAccessibleObject {
			DecodingInstance() {
				super(ClassAccess.this);
			}
			ObjectType[] types;
			@Override
			public Object getAssignable() throws BaseException {
				if(types!=null) for(ObjectType type:types) {
					TypeDefinition def=TYPE_MAP.get(type);
					if(def!=null) {
						return def;
					}
				}
				throw new BaseException(BaseException.NOT_FOUND,"primitive type");
			}		
		}
	}
}
