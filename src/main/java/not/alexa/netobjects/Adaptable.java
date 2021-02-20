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

import java.util.HashMap;
import java.util.Map;

/**
 * Objects of classes implementing this interface may provide additional facades to
 * other types. This is useful in a variety of situations:
 * <ul>
 * <li>Other libraries enlarge the capabilities of the object. The implementation can
 * be stored as an adapter and retrieved later on. Typically, this is for caching reasons.
 * <li>Other libraries uses the object and want to cache some optimized access.
 * <li>The object serves as a resource holder for others.
 * </ul>
 * 
 * <br>Implizitly, an object of the given class can be casted to another object if it is
 * either castable by java standard mechanism or provide an adapter of the given class.
 * 
 * 
 * 
 * @author notalexa
 *
 */
public interface Adaptable extends Castable {
	/**
	 * Get an adapter of the given class
	 * @param <T> the type of the adapter
	 * @param clazz the clazz
	 * @return an object of the given class related to this object
	 */
	public <T> T getAdapter(Class<T> clazz);
	
	/**
	 * Register an adapter on this object. The clazz and object parameters should
	 * fulfill the relation <code>true==clazz.isInstance(o)</code>.
	 * 
	 * @param clazz the class, the given object is an adapter for
	 * @param o the adapter object
	 */
	public void putAdapter(Class<?> clazz,Object o);
	
	/**
	 * Convenience method for <code>putAdapter(o.getClass(),o)</code>.
	 * 
	 * @param o the object to register
	 */
	public default  void putAdapter(Object o) {
		if(o==null) {
			throw new NullPointerException();
		}
		putAdapter(o.getClass(),o);
	}
	
	/**
	 * Typically, the method returns <code>this</code> if this object is castable to <code>clazz</code>
	 * as a java object or the given adapater (if any) otherwise.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public default <T> T castTo(Context context,Class<T> clazz) {
		if(clazz.isInstance(this)) {
			return (T)this;
		}
		return getAdapter(clazz);
	}
	
	/**
	 * Default implementation of the interface. In general, this class can be used
	 * as the base class of other implementations
	 * 
	 * @author notalexa
	 *
	 */
	public class Default implements Adaptable {
		protected Map<Class<?>,Object> adapters=null;
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getAdapter(Class<T> clazz) {
			return adapters==null?null:(T)adapters.get(clazz);
		}

		@Override
		public synchronized void putAdapter(Class<?> clazz, Object o) {
			if(o==null) {
				if(adapters!=null) {
					adapters.remove(clazz);
				}
			} else if(clazz.isInstance(o)) {
				if(adapters==null) {
					adapters=new HashMap<Class<?>, Object>();
				}
				adapters.put(clazz,o);
			}
		}
	}
}
