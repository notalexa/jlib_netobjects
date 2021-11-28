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

import java.lang.reflect.Modifier;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.TypeLoader;

/**
 * Constructor interface for the access framework. The constructor gets an {@link AccessContext}
 * to obtain additional informations (like objects on the stack).
 * 
 * @author notalexa
 *
 */
public interface Constructor {
    
    /**
     * Create a new instance for the given type
     * 
     * @param context the access context to use
     * @return an accessible object
     * @throws BaseException if an error occurs
     */
    public Object newInstance(AccessContext context) throws BaseException;
    
    /**
     * Does this constructor represents an overlay type. If {@code true}, the
     * class is either itself an overlay or inner class of a class which has an overlay.
     *  
     * @param loader the loader to resolve overlays for outer classes if needed
     * @return {@code true} if the constructor constructs an object of an overlay
     */
    public boolean isOverlay(TypeLoader loader);
   
    /**
     * The default constructor calls the {@link Class#newInstance()} method to
     * obtain an object. <i>This implementation is under construction</i>.
     * 
     * @author notalexa
     *
     */
    public class DefaultConstructor implements Constructor {
        Type type;
        Class<?> clazz;
        public DefaultConstructor(Type type,Class<?> clazz) {
            this.type=type;
            this.clazz=clazz;
        }
        @Override
        public Object newInstance(AccessContext context) throws BaseException {
            try {
                return clazz.newInstance();
            } catch(Throwable t) {
                return BaseException.throwException(t);
            }
        }
        @Override
        public boolean isOverlay(TypeLoader loader) {
            if(clazz.getAnnotation(Overlay.class)!=null) {
                return true;
            } else if(clazz.getEnclosingClass()!=null&&loader.hasOverlays(clazz.getEnclosingClass())) {
                return Modifier.isStatic(clazz.getModifiers());
            }
            return false;
        }
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
        public Object newInstance(AccessContext context) throws BaseException {
            if(type.hasOverlays()) {
                return context.resolve(context.getContext(), type).newInstance(context);
            } else {
                return delegate.newInstance(context);
            }
        }
        
        @Override
        public boolean isOverlay(TypeLoader loader) {
            // Should be called
            return false;
        }
    }
}
