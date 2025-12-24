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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Permission;
import java.util.Locale;

import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader;
import not.alexa.netobjects.types.access.AccessFactory;

/**
 * In many situations, the behavior of a class depends on a context in which methods
 * are evaluated. For example, the <code>getContextClassLoader()</code> tries to
 * defines a context dependent class loader. This is especially true in the case of
 * networking. The result depend on settings defined on the client side and
 * restrictions due to limited access of the client. A typical example for specific settings
 * is the language used in a call.
 * <br>This interface provides
 * <ul>
 * <li>Resources to calling methods
 * <li>Explicitly defined resources suitable in many situations
 * </ul>
 * 
 * @author notalexa
 *
 */
public interface Context extends Adaptable {
    
    /**
     * Create a root context with default values.
     * @return a root context
     */
    public static Context createRootContext() {
        return TypeLoader.createRootContext();
    }
    
    /**
     * Create a root context with the given type loader (and default values otherwise)
     * @param loader the type loader to use
     * @return a root context with the provided type loader
     */
    public static Context createRootContext(TypeLoader loader) {
        return new Root() {
            @Override
            public TypeLoader getTypeLoader() {
                return loader;
            }
        };
    }
    
	/**
	 * The type system defined in this library is essential for communication.
	 * <br>A context specific classloader can be obtained using {@link TypeLoader#getClassLoader()}.
	 * 
	 * @return the type loader of this context
	 */
	public default TypeLoader getTypeLoader() {
		return TypeLoader.DEFAULT_LOADER;
	}
	
	public default TypeDefinition resolveType(ObjectType type) {
		return getTypeLoader().resolveType(type);
	}
	
	public default TypeDefinition resolveType(Class<?> type) {
		return getTypeLoader().resolveType(type);
	}
	
	/**
	 * Method returning a logger for this context. In this library, logging is context and
	 * not class driven.
	 * 
	 * @return the logger of this context
	 */
	public default Logger getLogger() {
		return Root.LOG;
	}
	
	/**
	 * A context is the perfect place to control casting an object.
	 * 
	 * @param <T> the type 
	 * @param clazz the clazz the object should be casted to
	 * @param o the object which should be casted
	 * @return an object of the given type.
	 * 
	 * @see Castable#castTo(Context, Class)
	 */
	@SuppressWarnings("unchecked")
	public default <T> T cast(Class<T> clazz,Object o) {
		if(o instanceof Castable) {
			return ((Castable)o).castTo(this,clazz);
		} else if(clazz.isInstance(o)) {
			return (T)o;
		} else {
			return null;
		}
	}

	/**
	 * A context is the perfect place to control casting an object.
	 * 
	 * @param <T> the type 
	 * @param clazz the clazz the object should be casted to
	 * @param o the object which should be casted
	 * @return an object of the given type.
	 * @throws BaseException if the object cannot be cast to the requested class
	 * 
	 * @see Castable#failableCast(Context, Class)
	 */
	@SuppressWarnings("unchecked")
	public default <T> T failableCast(Class<T> clazz,Object o) throws BaseException {
		if(o instanceof Castable) {
			return ((Castable)o).failableCastTo(this,clazz);
		} else if(clazz.isInstance(o)) {
			return (T)o;
		} else {
			throw new BaseException(424,"Cannot cast "+o+" to "+clazz.getName());
		}
	}
	
	/**
	 * Simple permission evaluation. The default allows everything.
	 * 
	 * @param perm the permission to check
	 * @return <code>true</code> if the permission is granted in this context
	 */
	public default boolean implies(Permission perm) {
		return true;
	}
	
	/**
	 * Each context defines it's own locale. In the default implementation, this locale
	 * is the systems default lcoale.
	 * 
	 * @return the locale of this context.
	 */
	public default Locale getLocale() {
		return Locale.getDefault();
	}

	/**
	 * Cast this context to the given class
	 * @param <T> the requested type
	 * @param clazz the requested type
	 * @return this context casted to the requested class if possible or <code>null</code> if this is not possible
	 */
	public default <T> T castTo(Class<T> clazz) {
	    return castTo(this,clazz);
	}
	
    /**
     * Cast this context to the given class
     * @param <T> the requested type
     * @param clazz the requested type
     * @return this context casted to the requested class if possible
     * @throws BaseException if this context is not castable
     */
    public default <T> T failableCastTo(Class<T> clazz) throws BaseException {
        return failableCastTo(this,clazz);
    }
    
    public default <T> T upcast(T o) {
        return AccessFactory.getDefault().upcast(this,o);
    }

	/**
	 * Basic implementation of a context serving as a root context.
	 * 
	 * @author notalexa
	 *
	 */
	public class Root extends Adaptable.Default implements Context {
	    private static Logger LOG=LoggerFactory.getLogger(Context.class);
	}
	
	/**
	 * Basic implementation of a context which has a parent context. In general,
	 * a context used for evaluating a method for example consists of a chain of
	 * context beginning at a root context. Each context can request it's parent but
	 * cannot change it. The topmost context can be modified by the calling method and
	 * is instantiated for exactly this call.
	 * 
	 * @author notalexa
	 *
	 */
	public class Default extends Adaptable.Default implements Context {
		
		protected Context parent;
		
		public Default(Context parent) {
			if(parent==null) {
				throw new NullPointerException("Parent context");
			}
			this.parent=parent;
		}
		
		@Override
		public <T> T castTo(Context context, Class<T> clazz) {
			T t=super.castTo(context,clazz);
			return t!=null?t:parent.castTo(context, clazz);
		}

		@Override
		public TypeLoader getTypeLoader() {
			return parent.getTypeLoader();
		}

		@Override
		public Logger getLogger() {
			return parent.getLogger();
		}

		@Override
		public <T> T getAdapter(Class<T> clazz) {
			T t=super.getAdapter(clazz);
			if(t==null&&parent!=null) {
				return parent.getAdapter(clazz);
			}
			return t;
		}
	}
}
