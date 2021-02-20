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

import not.alexa.netobjects.Castable;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.ClassTypeDefinition.ClassAccess;

/**
 * A class providing the access framework with dynamic data.
 * 
 * @author notalexa
 * @see Access#newInstance(AccessContext)
 */
public interface AccessContext extends Castable {
	/**
	 * 
	 * @return the current context
	 */
	public Context getContext();
	/**
	 * Cast this context. This is highly dynamic. For example, the
	 * context should be castable to objects which are parent of the 
	 * object under consideration. This implements inner classes. For
	 * an non trivial usage example, compare with {@link ClassAccess}
	 * 
	 * @param <T> the type of the class
	 * @param clazz the requested class
	 * @return an object of the class or <code>null</code> if the
	 * current context cannot be cast to the requested type
	 * 
	 */
	public default <T> T castTo(Class<T> clazz) {
		return castTo(getContext(),clazz);
	}
}
