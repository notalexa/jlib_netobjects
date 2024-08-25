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
package not.alexa.netobjects.types;

import not.alexa.netobjects.types.access.DeferredAccess;

/**
 * Deferred objects are created when a type is either unknown at parsing time or the type is not
 * linked locally to a class. In the first case, the member is of type {@link DeferredObject} and the
 * class handles resolution of the type programmatically. In the second case, the member type is either {@code Object}
 * or an interface. In this case, the member can be assigned either a proxy of the interface of the deferred object
 * itself. Typical examples for the second case are:
 * <ul>
 * <li>Proxies on the object layer. The proxy doesn't need to know the class itself but may want to check if the
 * type is admissable.
 * <li>Callbacks in asynchronous apis. The callback is send back to the caller after the call succeeds but the implementor
 * doesn't need to known the precise structure of the callback.
 * </ul>
 * 
 * @author notalexa
 * 
 * @see DeferredObject
 * @see DeferredAccess
 */
public interface Deferred {
	/**
	 * The object type of the deferred object in a given namespace if known.
	 * @param ns the namespace
	 * @return the object type of the object if known, {@code null} otherwise.
	 */
	public ObjectType getObjectType(Namespace ns);
}
