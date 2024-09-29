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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import not.alexa.netobjects.BaseException;

/**
 * General interface representing a reference to an object. It's possible to create an {@link Editor} to get <b>a global lock</b> for modifying the object. Global depends
 * on the scope of the object itself. The default implementation assumes a unique correspondence between object and handle (which is typically wrong of course). The editor object is typically
 * loosely coupled to the object itself. It's the responsibility of the application to ensure, that modifications on the object and proper calls to {@link Editor#invalidate()}, {@link Editor#close()}
 * are made to ensure overall consistency.
 *  
 * @author notalexa
 * @param <K> the type of the object this handle represents
 */
public interface Handle<K> {
	/**
	 * 
	 * @return the object this handle represents
	 */
	public K get();

	/**
	 * Add a synchronization listener for this object. It's save not to remove the listener. It will be removed if garbage collected.
	 * 
	 * @param listener the listener to add
	 */
	public void addListener(SynchronizationListener<K> listener);

	/**
	 * 
	 * @param listener the listener to remove
	 */
	public void removeListener(SynchronizationListener<K> listener);

	/**
	 * The default implementation assumes that this handle is the only handle referencing {@link #get()}.
	 * Listeners added to this editor object will be removed after {@link Editor#close()}.
	 *  
	 * @param timeout the timeout for blocking
	 * @param unit the time unit
	 * @return an editor waiting for the given time or {@code null} if a timeout occurs
	 * @throws an exception if the editor cannot be created (because the handle is read only for example)
	 */
	public default Editor<K> createEditor(long timeout,TimeUnit unit) throws BaseException {
		return new Editor<K>() {
			private List<SynchronizationListener<K>> listeners;

			@Override
			public K get() {
				return Handle.this.get();
			}

			@Override
			public synchronized void addListener(SynchronizationListener<K> listener) {
				if(listeners==null) {
					listeners=new ArrayList<Handle.SynchronizationListener<K>>();
				}
				listeners.add(listener);
				Handle.this.addListener(listener);
			}

			@Override
			public void removeListener(SynchronizationListener<K> listener) {
				Handle.this.removeListener(listener);
			}

			@Override
			public void close() {
				if(listeners!=null) for(SynchronizationListener<K> l:listeners) {
					removeListener(l);
				}
			}

			@Override
			public void invalidate() {
			}
		};
	}
	
	/**
	 * @return an editor blocking until a lock can be returned.
	 * @throws an exception if the editor cannot be created (because the handle is read only for example)
	 */
	public default Editor<K> createEditor() throws BaseException {
		return createEditor(Long.MAX_VALUE,TimeUnit.MILLISECONDS);
	}
	
	/**
	 * An editor object for the given object. This object should synchronize between different handles.
	 * 
	 * @param <K> the type of the object this handle represents
	 */
	public interface Editor<K> extends AutoCloseable, Handle<K> {
		/**
		 * Close this editor. After close, no more modifications should be made.
		 */
		public void close();
		/**
		 * Invalidate the state of the object. If invalidated, the object is assumed to be modified and possible listeners are informed that the object changed.
		 */
		public void invalidate();		
	}
	
	/**
	 * Interface representing objects interested in modifications on an object.
	 *  
	 * @param <K> the type of the object
	 */
	public interface SynchronizationListener<K> {
		public void modified(K k);		
	}	
}
