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
import not.alexa.netobjects.api.Final;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;

/**
 * A placeholder for unresolvable object types. 
 * 
 * @author notalexa
 *
 */
@Final
public class UnknownTypeDefinition extends TypeDefinition {
	public static ClassTypeDefinition getTypeDescription() {
		return Types.UNKNOWN_TYPE;
	}
	
	
	public UnknownTypeDefinition(ObjectType... types) {
		super(types);
	}
		
	/**
	 * @return the constant {@link Flavour#UnknownType}
	 */
	@Override
	public Flavour getFlavour() {
		return Flavour.UnknownType;
	}
	
	/**
	 * Class access for unknown type definition
	 * 
	 * @author notalexa
	 *
	 */
	public static class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory) {
			super(factory,getTypeDescription());
		}
		
		@Override
		public AccessibleObject newAccessible(AccessContext context) throws BaseException {
			return new DefaultAccessibleObject(this,new UnknownTypeDefinition());
		}
		
		@Override
		public Object getField(Object o, int index) throws BaseException {
			UnknownTypeDefinition def=(UnknownTypeDefinition)o;
			switch(index) {
				case 0:return def.getTypes().toArray(new ObjectType[0]);
			}
			return null;
		}

		@Override
		public void setField(Object o, int index, Object v) throws BaseException {
			UnknownTypeDefinition def=(UnknownTypeDefinition)o;
			switch(index) {
				case 0:def.addTypes((ObjectType[])v);
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
