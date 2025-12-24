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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import not.alexa.netobjects.Context;

/**
 * The interface extends {@code InvocationHandler} and provides default implementation for
 * the invoke method:
 * <ul>
 * <li>It calls methods on the implementor if the declaring class is an assignable from the implementor.
 * <li>It calls the method on the {@link #resolveObject(Context, Method)} if the first argument is a context and the resolved object is not {@code null}
 * <li>It calls default methods.
 * <li>It calls {@link #handleUncallableMethod(Object, Method, Object[])} in all other cases.
 * </ul>
 * 
 * @author notalexa
 */
public interface InvocationSupport extends InvocationHandler {
	
	@Override
	public default Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(method.getDeclaringClass().isAssignableFrom(getClass())) {
			return method.invoke(this, args);
		} else {
			if(args!=null&&args.length>0&&args[0] instanceof Context) {
				Object o=resolveObject((Context)args[0],method);
				if(o!=null) {
					return method.invoke(o, args);
				}
			}
			if(method.isDefault()) {
				return callDefault(proxy, method, args);
			} else {
				return handleUncallableMethod(proxy, method, args);
			}
		}
	}
	
	/**
	 * Resolve the calling object of this invocation. The default return value can be {@code null} in which case either the default implementation (if any) is called
	 * or {@link #handleUncallableMethod(Object, Method, Object[]) is invoked.
	 *  
	 * @param context the context in which the object should be resolved (that's the first argument of the method call)
	 * @param m the method for which the calling object should be resolved (typically an object of the declaring class of {@code m})
	 * @return the calling object or {@code null} if unknown.
	 */
	public default Object resolveObject(Context context,Method m) {
		return null;
	}
	
	/**
	 * Call this method in case the method is not implemented by the implementor of this interface or
	 * is a default method.
	 * 
	 * @param proxy the proxy class
	 * @param method the method itself
	 * @param args the arguments
	 * @return the return value of the call.
	 * @throws Throwable if the method cannot be handled.
	 */
	public default Object handleUncallableMethod(Object proxy, Method method, Object[] args) throws Throwable {
		throw new RuntimeException("Uncallable: "+method);
	}
	
	/**
	 * Call a default method (not intended to be overwritten).
	 * 
	 * @param proxy the proxy
	 * @param m the method
	 * @param args the arguments
	 * @return the result of the call
	 * @throws Throwable if an error occurs
	 */
	public default Object callDefault(Object proxy,Method m,Object[] args) throws Throwable {
	    final float version = Float.parseFloat(System.getProperty("java.class.version"));
        if (version <= 52) {
            final java.lang.reflect.Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
            constructor.setAccessible(true);

            final Class<?> clazz = m.getDeclaringClass();
            return constructor.newInstance(clazz)
                    .in(clazz)
                    .unreflectSpecial(m, clazz)
                    .bindTo(proxy)
                    .invokeWithArguments(args);
        } else {
            return MethodHandles.lookup()
                    .findSpecial(
                            m.getDeclaringClass(),
                            m.getName(),
                            MethodType.methodType(m.getReturnType(), m.getParameterTypes()),
                            m.getDeclaringClass()
                    ).bindTo(proxy)
                    .invokeWithArguments(args);
        }
	}
}
