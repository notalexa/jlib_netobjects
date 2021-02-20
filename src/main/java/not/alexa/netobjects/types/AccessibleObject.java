/*
 * Copyright (C) 2020 Not Alexa
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
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.utils.Sequence;

/**
 * Typed objects are nice but have the disadvantage of being static. This interface introduces methods to:
 * <ul>
 * <li>Set and get fields from an object of type {@link Flavour#ClassType}.
 * <li>Add and iterate over elements of an object of {@link Flavour#ArrayType}
 * <li>Call methods on objects of {@link Flavour#MethodType}.
 * <li>Obtain objects of {@link Flavour#MethodType}.
 * </ul>
 * 
 * @author notalexa
 *
 * @see TypedObject
 */
public interface AccessibleObject extends TypedObject {
	/** 
	 * Set a field on this object (which must be of type {@link Flavour#ClassType}).
	 * 
	 * @param f the field to set
	 * @param value the (new) value of the field
	 * @throws BaseException if an error occurs
	 */
	public default void setField(Field f,AccessibleObject value) throws BaseException {
		throw new BaseException(BaseException.BAD_REQUEST, "Field "+f.getName()+" is unknown in "+getType());
	}
	
	/**
	 * Get the value of a field on this object (which must be of type {@link Flavour#ClassType}).
	 * 
	 * @param f the field to get
	 * @return the value of the field
	 * @throws BaseException if an error occurs
	 */
	public default AccessibleObject getField(Field f) throws BaseException {
		throw new BaseException(BaseException.BAD_REQUEST, "Field "+f.getName()+" is unknown in "+getType());
	}
	
	/**
	 * Optimization method. Get the value of a field which is either primitiv or an enumeration. In case of an enumeration, the 
	 * type <b>must</b> have a local (java) representation and a value of this class is returned.
	 *  
	 * @param f the field
	 * @return the (primitiv or enum) value of the field. This method may retnrn <code>null</code> of course.
	 * 
	 * @throws BaseException if an error occurs. Possible conditions are:
	 * <ul>
	 * <li>The field doesn't exist.
	 * <li>The field is not a primitiv or enumeration type.
	 * <li>The enumeration type has no (java) representation or the requested value is not a value of the local representation
	 * </ul>
	 */
	public default Object getPrimitiveOrEnumField(Field f) throws BaseException {
		switch(f.getType().getFlavour()) {
		case PrimitiveType:return getField(f).getObject();
		case EnumType:Object o=getField(f).getObject();
			if(o==null||Enum.class.isInstance(o)) {
				return o;
			} else {
				throw new BaseException(BaseException.BAD_REQUEST, getType()+": Type "+f.getType()+" not linked to a local representation.");
			}
			default: throw new BaseException(BaseException.BAD_REQUEST, getType()+": Type "+f.getType()+" is neither primitiv nor an enumeration type.");
		}
	}
	
	/**
	 * Retrieve an object representing the given method on this object (which must be of type {@link Flavour#ClassType} or {@link Flavour#InterfaceType}).
	 * Once retrieved, the method can be invoked using the {@link #call(Context, AccessibleObject...)} method with appropriate arguments.
	 * 
	 * @param method the method to obtain
	 * @return an object representing the method (based on <code>this</code> object)
	 * @throws BaseException if an error occurs
	 */
	public default AccessibleObject getMethod(MethodTypeDefinition method) throws BaseException {
		throw new BaseException(BaseException.BAD_REQUEST,"Method "+method +" is unknown in "+getType());
	}
	
	/**
	 * Add an element to this object (which must be of type {@link Flavour#ArrayType}.
	 * 
	 * @param o the object to add
	 * @throws BaseException if an error occurs
	 */
	public default void add(AccessibleObject o) throws BaseException {
		throw new BaseException(BaseException.BAD_REQUEST,getType()+" is not an array");
	}

	/**
	 * Viewed as a sequence, this object is either a singelton (in case the object is not of type {@link Flavour#ArrayType}) or
	 * the sequence of all elements in the array (if this object is of type {@link Flavour#ArrayType}).
	 * 
	 * @return the object as a sequence representing a singelton sequence or the array elements of this object or the underlying objects (if any)
	 * if this is a network object of network type
	 */
	public Sequence<AccessibleObject> asSequence();	

	/**
	 * Once obtained via {@link #getMethod(MethodTypeDefinition)}, we can invoke the method via this call. Note that network objects are
	 * supposed to "move around the world" to find the best place (using the given context) for evaluation returning the calculated values
	 * to the calling place (or client). Therefore, this method is highly flexible.
	 * <br>For example, in a UI, nothing needs to be known from an object except the attributes. Using for example a stylesheet, the attributes
	 * can be linked against an HTML page and manipulation can be done using JavaScript. The underlying object of the given type has
	 * an interface (added locally via the {@link ClassTypeDefinition#addInterface(InterfaceTypeDescription...)} method for example). For the manipulated object, the "store"
	 * method is retrieved (defined in the added interface) and executed via invocation of this {@link #call(Context, AccessibleObject...)} method.
	 * This results in an indirect call to another "place", where the method is implemented and the object is really stored (in a database for example).
	 * The UI didn't need to know any details of any methods on the object itself and is therefore independent of any concrete (java) representation
	 * and therefore independent of the servers technology stack.
	 * 
	 * @param context the context to use for evaluation
	 * @param params the call parameters
	 * @return the result of the call
	 * @throws BaseException if an error occurs
	 */
	public default AccessibleObject[] call(Context context,AccessibleObject...params) throws BaseException {
		throw new BaseException(BaseException.BAD_REQUEST,getType()+" is not a method");
	}
	
	/**
	 * {@link #getObject()} defines a generic object representing the fields of a given object. In general, this fields forms a tree which is
	 * dependent on the base element introducing new objects via the {@link #getField(Field)} method. Therefore, in some sense, the root
	 * object controls the concrete representation of the object. The return value of this method must be the "normal" representation
	 * for primitive and array types and a "suitable" representation for class, array and interface types.
	 * <br>In general, {@link #getObject()} is suitable but in special cases, some additional preparation has to be done. For example, if
	 * an object is not linked to a java type, but the type is an interface, a proxy may be created.
	 * 
	 * @return a suitable object of type for (field) assignments.
	 * @throws BaseException if the assignable object cannot be created (because of security reasons for example)
	 */
	public default Object getAssignable() throws BaseException {
		return getObject();
	}
	
	public abstract class Adapter implements AccessibleObject,Sequence<AccessibleObject> {

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
}
