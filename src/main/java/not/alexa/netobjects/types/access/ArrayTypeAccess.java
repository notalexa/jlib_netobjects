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
package not.alexa.netobjects.types.access;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.utils.Sequence;

/**
 * Access implementation for array types.
 * 
 * @author notalexa
 *
 */
public class ArrayTypeAccess extends Access.AbstractAccess implements Access {
	
	/**
	 * Canonicalize an array which is assigned to an arry type. Currently, the
	 * following types are supported:
	 * <ul>
	 * <li>Arrays
	 * <li>Sets
	 * <li>Lists
	 * <li>Maps (as {@link Map#entrySet()})
	 * 
	 * @param o the object to canoncalize
	 * @return the canonicalized object
	 * @throws BaseException if an error occurs (if the object
	 * is not an instance of the supported types for example)
	 */
	public static Collection<?> canonicalize(Object o) throws BaseException {
		if(o==null) {
			return null;
		} else if(o instanceof Collection) {
			return (Collection<?>)o;
		} else if(o instanceof Map) {
			return ((Map<?,?>)o).entrySet();
		} else if(o.getClass().isArray()) {
			if(o.getClass().getComponentType().isPrimitive()) {
				return new AbstractList<Object>() {
					int s=Array.getLength(o);
					@Override
					public int size() {
						return s;
					}

					@Override
					public Object get(int index) {
						return Array.get(o, index);
					}
				};
			} else {
				return Arrays.asList((Object[])o);
			}
		} else {
			throw new BaseException(BaseException.FORBIDDEN,"Unsupported array type "+o.getClass().getSimpleName());
		}
	}

	private TypeDefinition type;
	private Access componentAccess;
	private Class<?> targetClass;

	/**
	 * Create access to the given type definition. The type of the component access and the component type of the array type must match.
	 * 
	 * @param type the type definition of this access (with flavour {@link Flavour#ArrayType})
	 * @param componentAccess the access of the component type
	 * @param targetClass the target class of the array (either an array class, a collection or map)
	 */
	public ArrayTypeAccess(TypeDefinition type,Access componentAccess,Class<?> targetClass) {
		this.type=type;
		this.componentAccess=componentAccess;
		this.targetClass=targetClass;
	}
    
    @Override
    public AccessFactory getFactory() {
        return componentAccess.getFactory();
    }
	
	@Override
	public TypeDefinition getType() {
		return type;
	}

	/**
	 * Create a new accessible object. The {@link AccessibleObject#getAssignable()} method returns an object which can be casted to {@link #targetClass}.
	 *  
	 */
	@Override
	public AccessibleObject newAccessible(AccessContext context) throws BaseException {
		return new AccessibleArray(context);
	}
	
	/**
	 * Make a new accessible object. The {@link AccessibleObject#getAssignable()} method returns an object which can be casted to {@link #targetClass}.
	 *  
	 */
	@Override
	public AccessibleObject makeAccessible(AccessContext context,Object o) throws BaseException {
		return new AccessibleArray(context,o);
	}

	@Override
	public AccessibleObject makeDefault(AccessContext context,Object o) throws BaseException {
		if(o instanceof EmptyArray) {
			return ((EmptyArray)o).makeAccessible(context,this);
		} else {
			return super.makeDefault(context,o);
		}
	}

	
	@Override
	public Access getComponentAccess() throws BaseException {
		return componentAccess;
	}

	private class AccessibleArray implements AccessibleObject {
		private List<AccessibleObject> data;
		private AccessContext context;
		public AccessibleArray(AccessContext context) {
			this.data=new ArrayList<AccessibleObject>();
		}
		
		public AccessibleArray(AccessContext context,Object data) throws BaseException {
			this(context);
			for(Object o:canonicalize(data)) {
				this.data.add(componentAccess.makeAccessible(context,o));
			}
		}

		@Override
		public TypeDefinition getType() {
			return ArrayTypeAccess.this.getType();
		}

		@Override
		public Object getObject() {
			try {
				if(targetClass.isArray()) {
					int n=data.size();
					Object val=Array.newInstance(targetClass.getComponentType(),data.size());
					for(int i=0;i<n;i++) {
						Array.set(val,i, componentAccess.getObject(context,data.get(i)));
					}
					return val;
				} else if(List.class.isAssignableFrom(targetClass)) {
					List<Object> list;
					if(targetClass.isInterface()) {
						list=new ArrayList<Object>(data.size());
					} else try {
						list=(List<Object>)targetClass.newInstance();
					} catch(Throwable t) {
						return data;
					}
					for(AccessibleObject d:data) {
						list.add(componentAccess.getObject(context,d));
					}
					return list;
				} else if(Set.class.isAssignableFrom(targetClass)) {
					Set<Object> set;
					if(targetClass.isInterface()) {
						set=new HashSet<Object>();
					} else try {
						set=(Set<Object>)targetClass.newInstance();
					} catch(Throwable t) {
						return data;
					}
					for(AccessibleObject d:data) {
						set.add(componentAccess.getObject(context,d));
					}
					return set;
				} else if(Map.class.isAssignableFrom(targetClass)) {
					Map<Object,Object> map;
					if(targetClass.isInterface()) {
						map=new HashMap<Object, Object>();
					} else try {
						map=(Map<Object,Object>)targetClass.newInstance();
					} catch(Throwable t) {
						map=new HashMap<Object, Object>();
					}
					for(AccessibleObject d:data) {
						Map.Entry<Object,Object> entry=(Map.Entry<Object, Object>)componentAccess.getObject(context,d);
						map.put(entry.getKey(),entry.getValue());
					}
					return map;
				}
				return data;
			} catch(BaseException e) {
				return e.throwRuntimeException();
			}
		}

		@Override
		public void add(AccessibleObject o) throws BaseException {
			data.add(o);
		}

		@Override
		public Sequence<AccessibleObject> asSequence() {
			return Sequence.<AccessibleObject>from(data.iterator());
		}
	}
}
