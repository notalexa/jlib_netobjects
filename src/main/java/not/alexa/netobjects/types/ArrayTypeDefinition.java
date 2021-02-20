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

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;

/**
 * This network object type definition represents an array of network objects of the {@link #getComponentType()}. This definition is used
 * to map
 * <ul>
 * <li>Arrays
 * <li>Lists
 * <li>Sets
 * <li>Map entries
 * </ul>
 * In general, an array type definition has no explicit type.
 * 
 * @author notalexa
 *
 */
public class ArrayTypeDefinition extends TypeDefinition {
	public static ClassTypeDefinition getTypeDescription() {
		return Types.ARRAY_TYPE;
	}
	
	protected TypeDefinition componentType;
	private ArrayTypeDefinition() {}
	/**
	 * Construct an array for the given component type (which can be an array of cours).
	 * 
	 * @param componentType the component type of this array (should not be <code>null</code>)
	 * 
	 */
	public ArrayTypeDefinition(TypeDefinition componentType) {
		super();
		if(componentType==null) {
			throw new NullPointerException("component type");
		}
		this.componentType=componentType;
	}

	/**
	 * @return the constant {@link Flavour#ArrayType}
	 * 
	 */
	@Override
	public Flavour getFlavour() {
		return Flavour.ArrayType;
	}

	public TypeDefinition getComponentType() {
		return componentType;
	}
	
	public String toString() {
		return componentType.toString()+"[]";
	}
	
	public int hashCode() {
		return 0xf0f0f0f0^(componentType.hashCode()<<2);
	}
	
	public boolean equals(Object other) {
		if(other instanceof ArrayTypeDefinition) {
			return componentType.equals(((ArrayTypeDefinition)other).componentType);
		}
		return false;
	}

	/**
	 * Class access for an array type definition
	 * 
	 * @author notalexa
	 *
	 */
	public static class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory) {
			super(factory,getTypeDescription());
		}
		
		@Override
		public AccessibleObject newInstance(AccessContext context) throws BaseException {
			return new DefaultAccessibleObject(this,new ArrayTypeDefinition());
		}
		
		@Override
		public Object getField(Object o, int index) throws BaseException {
			ArrayTypeDefinition def=(ArrayTypeDefinition)o;
			switch(index) {
				case 0:return def.getTypes().toArray(new ObjectType[0]);
				case 1:return def.componentType;
			}
			return null;
		}

		@Override
		public void setField(Object o, int index, Object v) throws BaseException {
			ArrayTypeDefinition def=(ArrayTypeDefinition)o;
			switch(index) {
				case 0:def.addTypes((ObjectType[])v);
					break;
				case 1:def.componentType=(TypeDefinition)v;
					break;
			}
		}

		@Override
		public Access createFieldAccess(Field f) throws BaseException {
			switch(f.getIndex()) {
				case 0:return forArray(f.getType(),ObjectType[].class);
			}
			return super.createFieldAccess(f);
		}		
	}
}
