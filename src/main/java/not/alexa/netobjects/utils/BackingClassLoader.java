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
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Class loader based on {@linkplain URLClassLoader} implementing the {@link StrongRefPool} interface.
 * All strong references created by {@link #register(Reference)}, {@link #register(Class, Object)} and {@link #register(Class, Object, Runnable)}
 * are removed when this class loader is garbage collected.
 * <br> 
 * If {@link #isBackable(Class)} is {@code true} for a given class, instances can be registered relative to this class,
 * that is either within the classloader of the class implementing {@link StrongRefPool} or a default pool if
 * the class is loaded by the same or a parent of the class loader which loaded this framework.
 * <br>
 * {@link #register(Class, Object)} and {@link #register(Class, Object, Runnable)} returns a (weak) reference for
 * the given object. If {@link #isBackable(Class)} is {@link true}, a strong reference is created "fixing" the
 * given object as long as the class loader is alive.
 * 
 *   @author notalexa
 */
public class BackingClassLoader extends URLClassLoader implements StrongRefPool {
	private static ClassLoader ROOT_LOADER=BackingClassLoader.class.getClassLoader();
	private static StrongRefPoolSupport BASIC_SUPPORT=new StrongRefPoolSupport();
	
	private StrongRefPoolSupport poolSupport=new StrongRefPoolSupport();

	public BackingClassLoader(URL[] arg0) {
		super(arg0);
	}

	public BackingClassLoader(URL[] arg0, ClassLoader arg1) {
		super(arg0, arg1);
	}

	public BackingClassLoader(URL[] arg0, ClassLoader arg1, URLStreamHandlerFactory arg2) {
		super(arg0, arg1, arg2);
	}

	public <T extends Reference<?>> T register(T t) {
		return poolSupport.register(t);
	}

	/**
	 * Register the object {@code t} relative to the backing class (if possible). If the backing class doesn't
	 * allow to create a strong reference, the object <b>no reference is created</b>.
	 * 
	 * @param <T> the type of the object
	 * @param backingClass the backing class
	 * @param t the object to register
	 * @return a (weak) reference for the object.
	 */
	public static <T> Reference<T> register(Class<?> backingClass,T t) {
		return register(backingClass,t, null);
	}
	
	/**
	 * Register the object {@code t} relative to the backing class (if possible). If the backing class doesn't
	 * allow to create a strong reference, the object <b>no reference is created</b>.
	 * 
	 * @param <T> the type of the object
	 * @param backingClass the backing class
	 * @param t the object to register
	 * @param finalizer the finalizer to run after the object is removed from the pool. Invoking the finalizer <b>is
	 * not guaranteed</b>. If the class loader is garbage collected, is not invoked.
	 * @return a (weak) reference for the object.
	 */
	public static <T> Reference<T> register(Class<?> backingClass,T t,Runnable finalizer) {
		WeakReference<T> ref=finalizer==null?new WeakReference<>(t):new Finalizer<T>(t, finalizer);
		StrongRefPool pool=getRefPool(backingClass);
		return pool==null?ref:pool.register(ref);
	}
	
	/**
	 * @param backingClass the backing class
	 * @return {@code true} if the class can be used as a backing class (that is registered objects relative to
	 * this class get a strong reference to be prevented from garbage collection).
	 */
	public static boolean isBackable(Class<?> backingClass) {
		return getRefPool(backingClass)!=null;
	}
	
	private static StrongRefPool getRefPool(Class<?> clazz) {
		ClassLoader loader=clazz.getClassLoader();
		if(loader==null||loader==ROOT_LOADER) {
			return BASIC_SUPPORT;
		} else if(loader instanceof StrongRefPool) {
			return (StrongRefPool)loader;
		} else {
			ClassLoader rover=ROOT_LOADER.getParent();
			while(rover!=null) {
				if(rover==loader) {
					return BASIC_SUPPORT;
				}
				rover=rover.getParent();
			}
			return null;
		}
	}
	
	private static class Finalizer<T> extends not.alexa.netobjects.utils.Finalizer.Ref<T> implements Runnable {
		private Runnable r;
		public Finalizer(T referent, Runnable r) {
			super(referent);
			this.r=r;
		}
		
		@Override
		public void run() {
			if(r!=null) {
				r.run();
			}
		}
		
	}
}
