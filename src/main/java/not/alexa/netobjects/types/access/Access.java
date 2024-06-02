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
     * @return the factory which created this access
     */
    public AccessFactory getFactory();
	
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
	public AccessibleObject newAccessible(AccessContext context) throws BaseException;
	
	/**
	 * Construct an accessible object for the provided base object
	 * 
	 * @param o the object to wrap
	 * @return an accessible object
	 * @throws BaseException if an error occurs
	 */
	public default AccessibleObject makeAccessible(Object o) throws BaseException {
		return new DefaultAccessibleObject(this, o);
	}

	/**
	 * Construc an accessible object for the provided default object. The default implementation
	 * expects default objects to be "normal" objects but arrays and enumeration differ.
	 * 
	 * @param o the default value
	 * @return an accessible object representing the default value
	 * @throws BaseException if an error occurs
	 */
	public default AccessibleObject makeDefault(Object o) throws BaseException {
		return makeAccessible(o);
	}

	/**
	 * The counterpart of {@link #newAccessible(AccessContext)} and {@link #makeAccessible(Object)}.
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
	 * as an access, the value is the default value of the access (and used for {@link #newAccessible(AccessContext)}.
	 * 
	 * @author notalexa
	 *
	 */
	public static class SimpleTypeAccess implements Access, AccessibleObject,Sequence<AccessibleObject> {
		private static final ClassTypeDefinition.Field[] NO_FIELDS=new ClassTypeDefinition.Field[0];
		protected AccessFactory factory;
		protected TypeDefinition type;
		protected Object value;
		
		public SimpleTypeAccess(AccessFactory factory,TypeDefinition type) {
			this(factory,type,null);
		}
		
		public SimpleTypeAccess(AccessFactory factory,TypeDefinition type,Object value) {
		    this.factory=factory;
			this.type=type;
			this.value=value;
		}
		
        @Override
        public AccessFactory getFactory() {
            return factory;
        }
		
		@Override
		public TypeDefinition getType() {
			return type;
		}
		
		@Override
		public AccessibleObject newAccessible(AccessContext context) {
			return this;
		}
		
		@Override
		public AccessibleObject makeAccessible(Object v) throws BaseException {
			return new SimpleTypeAccess(factory,type, v);
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
	 * Access for types which do not have access. A typical example are interface types which can't be instantiated.
	 * Another example are types which are forbidden in the current context.
	 * 
	 * @author notalexa
	 *
	 */
	public class IllegalAccess implements Access {
        private AccessFactory factory;
	    private TypeDefinition type;
	   
	    /**
	     * Create an illegal access for the given type.
	     * 
	     * @param factory the access factory which creates this access
	     * @param type the type we need access for
	     */
	    public IllegalAccess(AccessFactory factory,TypeDefinition type) {
	        this.type=type;
	    }

        @Override
        public AccessFactory getFactory() {
            return factory;
        }

        @Override
        public TypeDefinition getType() {
            return type;
        }

        @Override
        public AccessibleObject newAccessible(AccessContext context) throws BaseException {
            throw new BaseException(BaseException.BAD_REQUEST,"Instance of type "+type);
        }
	}
}
