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
package not.alexa.netobjects.utils;

import java.lang.reflect.Array;

import not.alexa.netobjects.api.Final;
import not.alexa.netobjects.api.Overlay;

/**
 * Utility class for overlays
 * 
 * @author notalexa
 *
 */
public class OverlayUtils {
    private OverlayUtils() {}
    /**
     * Returns the class which is overloaded by <code>clazz</code>. The method throws a 
     * runtime exception if the overloaded class is {@link Final}.
     * 
     * @param clazz the class for which the overloaded class should be calculated 
     * @return the overloaded class of <code>clazz</code>
     */
    public static Class<?> resolve(Class<?> clazz) {
        if(clazz.isArray()) {
            return resolveArray(clazz);
        }
        Class<?> clazz0=clazz;
        Overlay overlay;
        while((overlay=clazz.getAnnotation(Overlay.class))!=null) {
            Class<?> overloaded=overlay.value();
            if(overloaded.equals(Object.class)) {
                clazz=clazz.getSuperclass();
            } else {
                clazz=overloaded;
            }
        }
        if(!clazz.equals(clazz0)) {
            if(clazz.getAnnotation(Final.class)!=null) {
                throw new RuntimeException("Final class "+clazz+" cannot be overloaded");
            }
            if(!clazz.isAssignableFrom(clazz0)||clazz.isInterface()) {
                throw new RuntimeException(clazz+" is not a superclass of "+clazz0);            
            }
        }
        return clazz;
    };
    
    private static Class<?> resolveArray(Class<?> ar) {
        Class<?> base=resolve(ar.getComponentType());
        return Array.newInstance(base, 0).getClass();
    }
}
