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

import not.alexa.netobjects.Castable;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.utils.Sequence;

/**
 * A typed object is an object with an associated type. No restrictions
 * are assumed on the object itself. 
 * <br>Let's introduce some terminology around typed objects. In principle, the {@link #getObject()} method may return
 * just something (for example a tree containing the attributes). In a lot of situations, the application want to obtain an
 * object of the java class which is "associated" with the type. To describe this, we say that <i>a java class is linked to a type
 * if this typed object can be casted to the given class</i>. In this case, we can use the (local) methods of the corresponding 
 * java class to implement functionality (for example functionality defined in a global method described via a {@link MethodTypeDefinition}.
 * The mechanism is flexible. Depending on the typed object and the context in use, the result may differ. Different java classes may be linked
 * in different situation to the same (global) type.
 * <br>To simplify the situation a little bit, we restrict the general mechanism of linkage in the following situations:
 * <ul>
 * <li>If the type is primitive, a linking class exist and is fixed. The linkage is described in the list in {@link PrimitiveTypeDefinition} and
 * the {@link #getObject()} method <b>always returns (<codenull</code> or) an instance of the linked type.
 * <li>If the type is an enumeration, the type is either not linked or linked against a java class which is a (java) enumeration.
 * </ul>
 * A unified access to the attributes (and global) methods of this instance are defined in {@link AccessibleObject}. Compare especially the
 * {@link Sequence} approach to arrays.
 * <br>If the type is not primitive and linked to a java class,
 * {@link #getObject()} may return an object of the given class but
 * this is required.
 * A safe way to obtain an
 * object of the given (linked) class is to cast it to that class.
 * 
 * @author notalexa
 *
 */
public interface TypedObject extends Castable {
	
	/**
	 * 
	 * @return the type of this typed object
	 */
	public TypeDefinition getType();
	
	/**
	 * The method returns (an internal representation of) this object. In case of a primitive type, the method <b>must</b> return an
	 * instance of the corresponding linked type, otherwise the object itself is undefined.
	 *  
	 * @return the object (data) of this typed object
	 */
	public Object getObject();
	
	/**
	 * In cases, the application wants a type even if the value itself is <code>null</code>, this method should return <code>true</code> and
	 * {@link #getObject()} should return <code>null</code>.
	 * 
	 * @return <code>true</code> if this object is null
	 */
	public default boolean isNull() {
		return false;
	}

	/**
	 * This (default) implementation uses the context to cast
	 * {@link #getObject()}.
	 */
	public default <T> T castTo(Context context, Class<T> clazz) {
		return context.cast(clazz, getObject());
	}
}
