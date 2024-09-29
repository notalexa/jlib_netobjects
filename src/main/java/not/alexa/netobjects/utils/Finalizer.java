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

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/**
 * Class providing References doing cleanup. This class is not directly accessible but is used by subclasses {@link Ref},
 * {@link SoftRef} and {@link PhantomRef}.
 * 
 * @author notalexa
 * @see Ref
 * @see SoftRef
 * @see PhantomRef
 */
public class Finalizer extends ReferenceQueue<Object> {
	private static final Finalizer INSTANCE=new Finalizer();
	private final Thread cleanUpThread=new Thread(() -> {
		while(true) try {
			Reference<?> ref=remove();
			if(ref instanceof Runnable) {
				((Runnable)ref).run();
			}
		} catch(Throwable t) {
			
		}
	}, "finalizer"); 

	private Finalizer() {
		cleanUpThread.setDaemon(true);
		cleanUpThread.start();
	}
	
	/**
	 * Abstract class extending a weak reference and implementing the {@code Runnable} interface.
	 * Put into the (unique) {@link Finalizer} queue.
	 * 
	 * @param <T> the type of the reference
	 */
	public static abstract class Ref<T> extends WeakReference<T> implements Runnable {
		public Ref(T referent) {
			super(referent,INSTANCE);
		}		
	}
	
	/**
	 * Abstract class extending a soft reference and implementing the {@code Runnable} interface.
	 * Put into the (unique) {@link Finalizer} queue.
	 * 
	 * @param <T> the type of the reference
	 */
	public static abstract class SoftRef<T> extends SoftReference<T> implements Runnable {
		public SoftRef(T referent) {
			super(referent,INSTANCE);
		}				
	}
	
	/**
	 * Abstract class extending a phantom reference and implementing the {@code Runnable} interface.
	 * Put into the (unique) {@link Finalizer} queue.
	 * 
	 * @param <T> the type of the reference
	 */
	public static abstract class PhantomRef<T> extends PhantomReference<T> implements Runnable {
		public PhantomRef(T referent) {
			super(referent,INSTANCE);
		}				
	}
}
