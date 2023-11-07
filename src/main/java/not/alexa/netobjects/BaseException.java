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
 * Most Java APIs define a more or less complex exception hierarchy do describe
 * different error conditions. In the context of network objects, the following
 * problem may arize: Suppose a network object is living in VM1. To evaluate a method
 * defined in some API A,
 * the object calls a remote method defined in the server VM2 which provide a remote implementation
 * of API A. In this VM, the method
 * is evaluated using API B which uses another remote method evaluated in VM3. In this
 * VM, an error condition arise described by an exception defined in API B. This is
 * thrown and travels back to VM2. If not suitable transformed, the exception may travel
 * through the network ariving in VM1. But VM1 doesn't know about API B and if the exception
 * is not contained in API A, the client cannot create an instance of this exception.
 * <br>To avoid this situation, the network object library restricts to a few exception.
 * This <code>BaseException</code> is the root exception in this framework.
 * <p>To take the network aspect into account, the exception defines (beside a messasge)
 * an error code, which is supposed to match an http return code whenever suitable, making
 * a translation to RESTful services easy.
 * 
 * 
 * @author notalexa
 *
 */
public class BaseException extends Exception {
	private static Map<Class<?>,Integer> DEFAULT_CODES=new HashMap<>();
	static {
		DEFAULT_CODES.put(SecurityException.class,403);
		DEFAULT_CODES.put(NullPointerException.class,404);
	}
	
	/**
	 * (Http) status code. The method call cannot be performed.
	 */
	public static final int BAD_REQUEST=400;

	/**
	 * Indicate a missing resource ({@linkplain NullPointerException} or {@linkplain ClassNotFoundException} for exanple).
	 */
	public static final int NOT_FOUND=404;
	
	/**
	 * The operation is forbidden. This is mapped to
	 * status code 406 (Not acceptable) while code 403 is
	 * mapped to {@link #NOT_AUTHORIZED}
	 */
	public static final int FORBIDDEN=406;
	
	/**
	 * The caller is not authorized for the operation.
	 * 
	 */
	public static final int NOT_AUTHORIZED=403;
	
	/**
	 * General error.
	 * 
	 */
	public static final int GENERAL=500;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The code of this exception
	 */
	private int code;
	
	public BaseException() {
	    this(GENERAL,(String)null);
	}

	public BaseException(int code,String s) {
		super(s);
		this.code=code;
	}
	
	private static String makeMessage(Throwable t) {
		return t.getMessage()==null||t.getMessage().length()==0?t.getClass().getSimpleName():t.getMessage();
	}

	private BaseException(int code,Throwable t) {
		super(makeMessage(t),t);
		this.code=code;
		setStackTrace(t.getStackTrace());
	}
	
	/**
	 * The code of an base exeption is supposed to reflect an HTTP error condition
	 * whenever suitable. Therefore, this code should be directly useful in RESTful
	 * services.
	 * 
	 * @return the return code of this exception 
	 */
	public int getCode() {
		return code;
	}

	/**
	 * In some situations, it's handy to create a base exception and throw it as
	 * a runtime exception since it's known that this exception is catched and
	 * the base exception is retransformed (using {@link #throwException(Throwable)}.
	 * 
	 * @throws RuntimeException created out of this exception
	 */
	public <T> T throwRuntimeException() throws RuntimeException {
		throw new RuntimeException(this);
	}

	/**
	 * Normalize a given exception. The
	 * method traverses the causes list of t and throws the first base exception
	 * found. If no base exception is found, a new exception is created with a
	 * suitable error code and the same stack trace as the original exception.
	 * 
	 * @param <T> the type of the return value (for convenience to formulate code
	 * like <code>return BaseException.throwException(t)</code> to clearify that the
	 * method will not evaluate code after this method.
	 * @param t the throwable to throw
	 * @return <code>the normalized exception/code>
	 * @throws BaseException always
	 */
	public static BaseException normalize(Throwable t) {
		while(t.getCause()!=null) {
			t=t.getCause();
		}
		if(t instanceof BaseException) {
			return (BaseException)t;
		} else {
			int code=500;
			Class<?> exceptionClass=t.getClass();
			Integer c=null;
			while(c==null&&Throwable.class.isAssignableFrom(exceptionClass)) {
				c=DEFAULT_CODES.get(exceptionClass);
				exceptionClass=exceptionClass.getSuperclass();
			}
			if(c!=null) {
				code=c;
			}
			return new BaseException(code,t);
		}
	}

	/**
	 * Throw a base exception created out of the given exception.
	 * 
	 * @param <T> the type of the return value (for convenience to formulate code
	 * like <code>return BaseException.throwException(t)</code> to clearify that the
	 * method will not evaluate code after this method.
	 * @param t the throwable to throw
	 * @return <code>null/code>
	 * @throws BaseException always
	 * @see #normalize(Throwable)
	 */
	public static <T> T throwException(Throwable t) throws BaseException {
		throw normalize(t);
	}
}
