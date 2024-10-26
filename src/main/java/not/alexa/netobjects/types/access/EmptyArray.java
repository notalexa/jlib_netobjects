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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;

/**
 * An {@code EmptyArray} represents an array default value with length 0.
 * This default value is generated setting the default value in the build API to {@code empty}.
 * 
 */
public class EmptyArray {
	private static final ClassTypeDefinition TYPE_DEFINITION=new ClassTypeDefinition(EmptyArray.class).createBuilder()
			.build();
	
	public static ClassTypeDefinition getTypeDescription() {
		return TYPE_DEFINITION;
	}
	
	public EmptyArray() {
	}
	
	public AccessibleObject makeAccessible(AccessContext context,Access access) {
		try {
			switch(((ArrayTypeDefinition)access.getType()).getArrayFlavour()) {
			case Map:return access.makeAccessible(context,new HashMap<Object,Object>());
			case Array:
				default:return access.makeAccessible(context,new ArrayList<Object>(0));
			
			}
		} catch(Throwable t) {
			return null;
		}
	}
	
	public boolean represents(Object o) {
		if(o.getClass().isArray()) {
			return Array.getLength(o)==0;
		} else if(o instanceof Collection) {
			return ((Collection<?>)o).size()==0;
		} else if(o instanceof Map) {
			return ((Map<?,?>)o).size()==0;
		} else {
			return false;
		}
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}
	
	public static abstract class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory) {
			super(factory,getTypeDescription());
		}
		
		@Override
		public AccessibleObject newAccessible(AccessContext context) throws BaseException {
			return new DefaultAccessibleObject(this,new EmptyArray());
		}
		
		@Override
		public Object getField(AccessContext context,Object o, int index) throws BaseException {
			return null;
		}

		@Override
		public void setField(AccessContext context,Object o, int index, Object v) throws BaseException {
		}
	}
}
