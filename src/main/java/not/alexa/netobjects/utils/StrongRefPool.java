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
package not.alexa.netobjects.utils;

import java.lang.ref.Reference;

/**
 * The interface is intended to backup a strong reference to the referent of the provided reference. The strong reference should become garbage collectable if
 * <ul>
 * <li>The pool becomes garbage collectable.
 * <li>All registered references of the referent becomes garbate collectable.
 * </ul>
 * Implementors of this interface usually extends or delegates to {@link StrongRefPoolSupport}.
 * <br>
 * This interface can be used in the following generic use cases:
 * <ol>
 * <li>An instance of class {@code B} based on class {@code A} is expensive to construct. If {@code A} implements this interface, a (weak) reference of {@code B} can
 * be registered at {@code A} and the instance will not be recycled until the instance of {@code A} is garbage collected. Note that {@code B} can hold strong references of {@code A} since
 * only weak references are shared. 
 * 
 * <li>An instance of a class {@code A} is created using a factory and holds a reference to the factory. The factory implements this interface and classes based on {@code A}
 * are expensive to construct. Registering this instances at the factory ensures that a weak reference will not be outdated until all references of all instances constructed by the
 * factory are removed. An example are the class {@code Class} and {@link BackingClassLoader}. A registered instanceof {@code B} will not be removed until all instances of all
 * classes loaded by the backing ClassLoader will be garbage collected. In combination with the first use case codecs based on access (and registered at this access) based on a class will not
 * be removed until the class loader itself is garbage collected (which implies that no instances of all classes loaded with this class loader are garbage collected).
 * 
 * <li>Caching and singeltons. If we have a "primary key" and want to provide a singleton, the caching mechanism can use this pool delivering a handle pointing to the singelton instance.
 * Caching can be provided by holding an explicit strong reference to the cached object (preventing it from being garbage collected). If the cache is fixed, all provided handles (based on a weak
 * reference) can be sure that the referent will not be deleted.
 * </ol>
 *  
 * @author notalexa
 * @see StrongRefPoolSupport
 * @see BackingClassLoader
 * @see Handle
 */
public interface StrongRefPool {

	/**
	 * Register a strong reference for the object referenced by {@code t}. This reference should be
	 * removed after the provided reference (and all other registered references referencing the same object)
	 * is garbage collected.
	 *  
	 * @param <T> the type of the object
	 * @param t the reference to the object
	 * @return the reference itself
	 */
	public <T extends Reference<?>> T register(T t);

}
