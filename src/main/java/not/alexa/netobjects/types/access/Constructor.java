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

import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.List;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.TypeResolver;

/**
 * Constructor interface for the access framework. The constructor gets an {@link AccessContext}
 * to obtain additional informations (like objects on the stack).
 * <br>The wording "constructor" is a little bit misleading. Originally intended to serve as a
 * factory for instances in the decoding processes, the constructor instance also provides information about
 * the mapping between type definitions and concrete classes. Currently, only (field) name mapping
 * is supported, see {@link #mapField(Class, String)}.
 * <p>The providers defined in this library consistently respect the following features:
 * <ul>
 * <li>if the class is a non static inner class, the enclosing instance is resolved from the {@link AccessContext}.
 * <li>if the class (or a superclass) has a method {@code Object finish(AccessContext)}, this method is called <b>after</b> setting all fields 
 * when using the {@link AccessibleObject#getAssignable()} method.
 * <li>class definition field names are mapped to class field names using the {@link FieldMapper} method.
 * </ul>
 * The following features are currently <b>not supported</b> (but under consideration).
 * <ul>
 * <li>Injection of objects.
 * <li>Injection of the parent object.
 * <li>Setting fields using getter/setter methods.
 * </ul>
 * 
 * @author notalexa
 *
 */
public interface Constructor {
	/**
	 * Add the provider for constructor resolution.
	 * @param provider the provider
	 * @see Provider
	 */
	public static void addProvider(Provider provider) {
		ConstructorHelper.add(provider);
	}
	
	/**
	 * Get the constructor for the given overlay.
	 * 
	 * @param clazz the underlying class
	 * @param overlay the overlay of the class
	 * @return a constructor for the overlay class
	 */
	public static Constructor get(LinkedLocal clazz,LinkedLocal overlay) {
		return ConstructorHelper.get(clazz, overlay);
	}
    
	public static Constructor create(Access access) {
		return ConstructorHelper.TRIVIAL_CONSTRUCTOR;
	}

	/**
	 * 
	 * @param clazz the class in question
	 * @return the enclosing class (this is only the java enclosing class if {@code clazz} is an inner class and not static)
	 */
	public static Class<?> getEnclosingClass(Class<?> clazz) {
        return Modifier.isStatic(clazz.getModifiers())?null:clazz.getEnclosingClass();
	}	

    /**
     * Create a new instance for the given type
     * 
     * @param context the access context to use
     * @return an accessible object
     * @throws BaseException if an error occurs
     */
    public PreAccessible newInstance(AccessContext context) throws BaseException;
    
    /**
     * Map a field. This method is typically used to <i>resolve the class field name from the type definition field name</i>
     * already knowing the class. It's save to return an arbitrary value if the field doesn't belong to the class.
     * 
     * @param clazz the class the field belongs to
     * @param fieldName the (type definition) field name
     * @return the (class) field name
     */
    public String mapField(Class<?> clazz,String fieldName);
    
    /**
     * A constructor typically depends on the underlying class. Therefore, if a {@link TypeResolver} would provide a constructor to
     * give hints how to create the instance, this constructor must be stored in the system. This implies a memory leak, since the class
     * will never be garbage collected even if never used. Therefore, a provider is used to serve as a factory for constructing a {@link Constructor}
     * for a given class and it's overlays.
     * 
     * <br>The provider uses a weak reference on the class to get informed if the class is recycled. The library keeps track of providers with unused classes
     * and removes them from the system. 
     * 
     * @author notalexa
     *
     */
    public abstract class Provider extends WeakReference<Class<?>> {
    	protected String clazzName;
    	Provider next;
    	
		protected Provider(Class<?> clazz) {
			super(clazz,ConstructorHelper.QUEUE);
			clazzName=clazz.getName();
			ConstructorHelper.add(this);
		}
		
		/**
		 * Resolve a constructor for the given overlay.
		 * 
		 * @param clazz the defining clazz of the overlay. We should have an identity {@link #get()} and {@link LinkedLocal#asClass()} for this parameter
		 * @param overlay the overlay
		 * @return a constructor for the overlay
		 */
		protected abstract Constructor resolve(LinkedLocal clazz, LinkedLocal overlay);
    }
    
    /**
     * This provider can be used if the class has a trivial constructor. The provided field map is used to resolve field names
     * 
     * @author notalexa
     * @see FieldMapper
     */
	public class DefaultProvider extends Provider {
		private FieldMapper fieldMap;
		/**
		 * 
		 * @param clazz the class this provider is attached to
		 * @param fieldMap the field mapper used to resolve class field names
		 */
		public DefaultProvider(Class<?> clazz,FieldMapper fieldMap) {
			super(clazz);
			this.fieldMap=fieldMap;
		}

		@Override
		protected Constructor resolve(LinkedLocal clazz, LinkedLocal overlay) {
			return new ConstructorHelper.DefaultConstructor(overlay,fieldMap);
		}
	}
	
	/**
	 * This provider can be used if a non trivial constructor is used to construct the class. The
	 * library defers instance creation until all fields are set needed for the constructor.
	 * 
	 * @author notalexa
	 *
	 */
	public abstract class DeferredProvider extends Provider {
		/**
		 * The (type definition) field names of the constructor parameters
		 */
		private List<String> constructorFields;
		private FieldMapper fieldMap;
		
		/**
		 * 
		 * @param clazz the class this provider is attached to
		 * @param constructorFields the list of constructor fields
		 * @param fieldMap the field mapper used to resolve class field names
		 */
		public DeferredProvider(Class<?> clazz,List<String> constructorFields,FieldMapper fieldMap) {
			super(clazz);
			this.constructorFields=constructorFields;
			this.fieldMap=fieldMap;
		}
		
		/**
		 * Find the constructor for the given class (this is the overlay class). This typically depends on the {@link TypeResolver}, who provides
		 * an implementation for this method.
		 * 
		 * @param enclosingClass the enclosing class, if clazz is an inner non static class
		 * @param clazz the overlay class we need a constructor for
		 * @return the constructor
		 * @throws Throwable if an error occurs
		 */
		protected abstract java.lang.reflect.Constructor<?> findConstructor(Class<?> enclosingClass, Class<?> clazz) throws Throwable;
		
		@Override
		protected Constructor resolve(LinkedLocal clazz, LinkedLocal overlay) {
			try {
				return constructorFields.size()==0?new ConstructorHelper.DefaultConstructor(overlay,fieldMap):new ConstructorHelper.DeferredConstructor(overlay,findConstructor(getEnclosingClass(overlay.asClass()),overlay.asClass()),constructorFields,fieldMap);
			} catch(Throwable t) {
				return new ConstructorHelper.DefaultConstructor(overlay,t);
			}
		}
	}

	/**
	 * Interface used to resolve the real name of a field.
	 * 
	 * @author notalexa
	 *
	 */
	public static interface FieldMapper {
		/**
		 * No mapping. The instance always returns the field name
		 */
		FieldMapper IDENTITY=new FieldMapper() {
			@Override
			public String mapField(Class<?> clazz, String fieldName) {
				return fieldName;
			}
		};
		
		/**
		 * Map the (type definition) field name to the class field name.
		 * 
		 * @param clazz the class to which the field belongs
		 * @param fieldName the field name
		 * @return the class field name
		 */
		public String mapField(Class<?> clazz,String fieldName);
	}

    /**
     * The overlay constructor checks if overlays exist for the given type and resolves
     * the constructor of the overlay class if necessary. Otherwise the constructor delegates
     * to the constructor of the type class.
     * 
     * @author notalexa
     *
     */
    public class OverlayConstructor implements Constructor {
        Type type;
        Constructor delegate;
		public OverlayConstructor(Type type,Constructor delegate) {
            this.delegate=delegate;
            this.type=type;
        }
        @Override
        public PreAccessible newInstance(AccessContext context) throws BaseException {
            if(type.hasOverlays()) {
                return context.resolve(context.getContext(), type).newInstance(context);
            } else {
                return delegate.newInstance(context);
            }
        }
        
        @Override
        public String mapField(Class<?> clazz, String fieldName) {
			return delegate.mapField(clazz, fieldName);
		}
    }
    
    /**
     * Internal interface denoting a preconstructed object.
     *  
     * @author notalexa
     *
     */
    public interface PreAccessible {
        /**
         * Create an accessbile object for this preconstructed object.
         * 
         * @param access the (compatible) access for this preconstructed object
         * @return the corresponding accessible object
         * @throws BaseException if the object cannot be created
         */
        public AccessibleObject makeAccessible(Access access) throws BaseException;
    }
}
