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
package not.alexa.netobjects;

/**
 * This self reflectional interface describes the ability of a class to change it's type
 * and can be compared with the usual Java cast operation. But the {@link #castTo(Context, Class)}
 * operation is considered to be more flexible in the following ways:
 * <br>It's not restricted to the normal class hierarchy:
 * <ul>
 * <li>A downcast is a cast to an interface implemented by the class or to a superclass of the class.
 * <li>An upcast is a cast to a class which can be downcasted to the given class.
 * <li>A cross cast is a cast to some other class not directly related to the given class. Cross casts
 * are used to provide more and different functionality to a given object. A typical example of cross casting
 * is the concept of adapters (see {@link Adaptable}.
 * </ul>
 * <br>It allows dependencies on a context. Typical context specific questions are:
 * <ul>
 * <li>Permissions: Is the cast allowed or not?
 * <li>Resources: Which class loader should be used for cross or up casting.
 * </ul>
 * This interface is intimately related to {@link Adaptable} providing a simple mechanism for
 * cross casting and {@link Context} providing an interface for "typical resources" needed.
 * 
 * @author notalexa
 * 
 * @see Adaptable
 * @see Context
 *
 */
public interface Castable {
	/**
	 * Cast myself to the given <code>clazz</code>. No assumptions are made on the class
	 * and no assumptions are made on success. This method never throws an error.
	 * If the object is not castable to the requested class, <code>null</code> should be returned.
	 * 
	 * @param <T> the type to cast to
	 * @param context the (resource) context to use for casting
	 * @param clazz the requested class
	 * @return <code>this</code> casted to <code>T</code>.
	 */
	public <T> T castTo(Context context,Class<T> clazz);
	
	/**
	 * Cast myself to the given <code>clazz</code>. On success, this method should return the same
	 * object as {@link #castTo(Context, Class)}. If the cast is not possible, this method should fail.
	 * <br>Classes overriding this method may provide more precise information on why this cast failed.
	 * 
	 * @param <T> the type to cast to
	 * @param context the (resource) context to use for casting
	 * @param clazz the requested class
	 * @return <code>this</code> casted to <code>T</code>.
	 * @throws BaseException if the object cannot be cast to the requested class
	 */
	public default <T> T fallibleCastTo(Context context,Class<T> clazz) throws BaseException {
		T t=castTo(context,clazz);
		if(t==null) {
			throw new BaseException(424,"Cannot cast "+this+" to "+clazz.getName());
		}
		return t;
	}
}
