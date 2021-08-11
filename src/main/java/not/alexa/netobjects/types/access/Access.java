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

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.utils.Sequence;

/**
 * The static counterpart to {@link AccessibleObject}. In general,
 * given a suitable access of type this type and an object,
 * an accessible object can be constructed by contracting the access
 * with the accessible object.
 * 
 * @author notalexa
 *
 */
public interface Access {
	
	/**
	 * 
	 * @return the underlying type definition of this access
	 */
	public TypeDefinition getType();
	
	/**
	 * The class loader used for this access. This is <b>not</b>
	 * the class loader used to load the access class. In general,
	 * this class is child of the class loader used to load the
	 * class.
	 * <p>Together with {@link #getType()}, this loader is
	 * the primary key of the access
	 * 
	 * @return the class loader of this 
	 */
	public default ClassLoader getAccessLoader() {
		return getClass().getClassLoader();
	}
	
	/**
	 * For a type representing the type of this access (that is the object type is contained in the set of {@link TypeDefinition#getTypes()},
	 * the method should return a key representing this access. Typically, the access is unique with respect to the type and {@link #getAccessLoader()}.
	 * For convenience, the method return the type itself if the loader is the class loader of the core library.
	 * 
	 * @param type the type we need the key for
	 * @return a unique key for the access
	 */
	public default Object getAccessKey(ObjectType type) {
	    ClassLoader accessLoader=getAccessLoader();
	    if(accessLoader==Access.class.getClassLoader()) {
	        return type;
	    } else {
	        return new AccessKey(type,accessLoader);
	    }
	}
	
	/**
	 * Finalize construction of o. The default implementation
	 * returns the object itself.
	 * 
	 * @param o the object to finalize
	 * @return the finalized object
	 * @see AccessibleObject#getAssignable()
	 */
	public default Object finish(Object o) {
		return o;
	}
	
	/**
	 * Convenience method to retrieve the fields of the type.
	 * The default method returns the fields of the type if
	 * the type is a class type and an empty array otherwise.
	 * 
	 * @return the fields of the type
	 */
	public default ClassTypeDefinition.Field[] getFields() {
		TypeDefinition descr=getType();
		if(descr.getFlavour()==Flavour.ClassType) {
			return ((ClassTypeDefinition)descr).getFields();
		}
		return SimpleTypeAccess.NO_FIELDS;
	}
	
	/**
	 * 
	 * @param o the object
	 * @param f the field
	 * @return the value of the field
	 * @throws BaseException if an error occurs
	 * @see AccessibleObject#getField(Field)
	 */
	public default Object getField(Object o,ClassTypeDefinition.Field f) throws BaseException {
		throw new BaseException(BaseException.BAD_REQUEST, "Field "+f.getName()+" is unknown in "+getType());
	}

	/**
	 * 
	 * @param o the object
	 * @param f the field
	 * @param v the new value
	 * @throws BaseException if an error occurs
	 * @see AccessibleObject#setField(Field, AccessibleObject)
	 */
	public default void setField(Object o,Field f, Object v) throws BaseException {
		throw new BaseException(BaseException.BAD_REQUEST, "Field "+f.getName()+" is unknown in "+getType());
	}

	/**
	 * Create a new instance for the given type
	 * 
	 * @param context the access context to use
	 * @return an accessible object
	 * @throws BaseException if an error occurs
	 */
	public AccessibleObject newInstance(AccessContext context) throws BaseException;
	
	/**
	 * Construct an accessible object for the provided base object
	 * 
	 * @param o the object to wrap
	 * @return an accessible object
	 * @throws BaseException if an error occurs
	 */
	public default AccessibleObject makeInstance(Object o) throws BaseException {
		return new DefaultAccessibleObject(this, o);
	}
	
	/**
	 * The counterpart of {@link #newInstance(AccessContext)} and {@link #makeInstance(Object)}.
	 * This method should return a finalized object, see {@link #finish(Object)}.
	 * <br>The default implementation returns {@link AccessibleObject#getAssignable()}
	 * 
	 * @param o the accessible object
	 * @return the corresponding raw object
	 * @throws BaseException if an error occurs
	 */
	public default Object getObject(AccessibleObject o) throws BaseException {
		return o.getAssignable();
	}
	
	/**
	 * 
	 * @param f the field
	 * @return the access for the field based on this access
	 * @throws BaseException if an error occurs (if the field doesn't belong
	 * to the type for example)
	 */
	public default Access getFieldAccess(Field f) throws BaseException {
		throw new BaseException(BaseException.BAD_REQUEST, "Field "+f.getName()+" is unknown in "+getType());
	}
	

	/**
	 * 
	 * @return the component access if the type of this access is an array.
	 * 
	 * @throws BaseException if an error occurs (if the type is not an array type for example)
	 */
	public default Access getComponentAccess() throws BaseException {
		throw new BaseException(BaseException.BAD_REQUEST, getType()+" is not an array type.");
	}

	/**
	 * Class suitable for primitive and enumeration types. This class implements {@link Access} and {@link AccessibleObject}. Viewed
	 * as an access, the value is the default value of the access (and used for {@link #newInstance(AccessContext)}.
	 * 
	 * @author notalexa
	 *
	 */
	public static class SimpleTypeAccess implements Access, AccessibleObject,Sequence<AccessibleObject> {
		private static final ClassTypeDefinition.Field[] NO_FIELDS=new ClassTypeDefinition.Field[0];
		protected TypeDefinition type;
		protected Object value;
		
		public SimpleTypeAccess(TypeDefinition type) {
			this(type,null);
		}
		
		public SimpleTypeAccess(TypeDefinition type,Object value) {
			this.type=type;
			this.value=value;
		}
		
		@Override
		public TypeDefinition getType() {
			return type;
		}
		
		@Override
		public AccessibleObject newInstance(AccessContext context) {
			return this;
		}
		
		@Override
		public AccessibleObject makeInstance(Object v) {
			return new SimpleTypeAccess(type, v);
		}

		@Override
		public Object getObject() {
			return value;
		}

		@Override
		public boolean busy() {
			return true;
		}

		@Override
		public AccessibleObject current() {
			return this;
		}

		@Override
		public Sequence<AccessibleObject> next() {
			return Sequence.emptySequence();
		}

		@Override
		public Sequence<AccessibleObject> asSequence() {
			return this;
		}
	}
	
	/**
	 * Class representing a key uniquely identifying access for a type.
	 * 
	 * @author notalexa
	 *
	 */
	public class AccessKey {
	    private ObjectType type;
	    private ClassLoader loader;
	    public AccessKey(ObjectType type,ClassLoader loader) {
	        this.type=type;
	        this.loader=loader;
	    }
        @Override
        public boolean equals(Object obj) {
            if(obj==this) {
                return true;
            } else if(obj instanceof AccessKey) {
                AccessKey other=(AccessKey)obj;
                return other.loader==loader&&other.type.equals(type);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return type.hashCode()^loader.hashCode();
        }
	}
}
