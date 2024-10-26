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
package not.alexa.netobjects.types.access;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;

/**
 * The class represents a default enumeration value and is inserted in the build API
 * to ensure interoperability.
 * <br>The problem is that an enumeration value depends on the class which depends on the
 * class loader which has not to be the default class loader. In this case, a type definition
 * loaded with a reference to the enumeration type cannot be loaded because the 
 * dependent type is unknown in the root class loader.
 * 
 */
public class EnumConstant {
	private static final ClassTypeDefinition TYPE_DEFINITION=new ClassTypeDefinition(EnumConstant.class).createBuilder()
			.createField("value",PrimitiveTypeDefinition.getTypeDescription(String.class)).addTag("xml","@value").build()
			.build();
	
	public static ClassTypeDefinition getTypeDescription() {
		return TYPE_DEFINITION;
	}
	
	protected String value;
	
	protected EnumConstant() {
	}
	
	public EnumConstant(String value) {
		this.value=value;
	}
	
	public AccessibleObject makeAccessible(AccessContext context,Access access) throws BaseException {
		Class<?> clazz=access.getType().getJavaClassType().asLinkedLocal(access.getAccessLoader()).asClass();
		return access.makeAccessible(context,Enum.valueOf(clazz.asSubclass(Enum.class), value));
	}
	
	public boolean represents(Object o) {
		return o instanceof Enum && o.toString().equals(value);
	}
	
	public String toString() {
		return value;
	}
	
	public static class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory) {
			super(factory,getTypeDescription());
		}
		
		@Override
		public AccessibleObject newAccessible(AccessContext context) throws BaseException {
			return new DefaultAccessibleObject(this,new EnumConstant());
		}
		
		@Override
		public Object getField(AccessContext context,Object o, int index) throws BaseException {
			EnumConstant enumConstant=(EnumConstant)o;
			switch(index) {
				case 0: return enumConstant.value;
			}
			return null;
		}

		@Override
		public void setField(AccessContext context,Object o, int index, Object v) throws BaseException {
			EnumConstant enumConstant=(EnumConstant)o;
			switch(index) {
				case 0:enumConstant.value=(String)v;
					break;
			}
		}
	}
}
