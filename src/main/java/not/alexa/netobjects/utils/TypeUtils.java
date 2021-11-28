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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.UUID;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Final;
import not.alexa.netobjects.api.NetworkObject;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;

/**
 * Utility class for overlays
 * 
 * @author notalexa
 *
 */
public class TypeUtils {
    private TypeUtils() {}
    
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
    
    /**
     * Builder utility to build names out of strings. The builder is able to provide a (unique) name for
     * a sequence of input data.
     * 
     * @return a new name builder
     * 
     */
    public static NameBuilder createNameBuilder() {
        return new NameBuilder();
    }
    
    /**
     * Name builder for building condensed names out of (complicated) data. For example, the 
     * builder can be used to create a unique name for a method (with name and parameters).
     * <p>The builder can be reused after performing one of the final operations:
     * <ul>
     * <li>{@link #asUUID()}.
     * </ul>
     * 
     * @author notalexa
     *
     */
    public static class NameBuilder {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        NameBuilder() {
        }
       
        /**
         * Add the bytes to the name
         * @param bytes the bytes to add.
         * @return this builder for additional operations
         */
        public NameBuilder add(byte[] bytes) {
            try {
                out.write(bytes);
            } catch(Throwable t) {
            }
            return this;
        }
        
        /**
         * Add the string to the name
         * @param s the string to add
         * @return this builder for additional operations
         */
        public NameBuilder add(String s) {
            if(s!=null) try {
                add(s.getBytes("UTF-8"));
            } catch(Throwable t) {
            }
            return this;
        }
        
        /**
         * 
         * @return a UUID representing this name
         */
        public UUID asUUID() {
            try {
                return UUID.nameUUIDFromBytes(out.toByteArray());
            } finally {
                out.reset();
            }
        }
    }

    /**
     * Resolve the (java class) type of the given object type
     * 
     * @param context the context to use for resolving
     * @param type the object type to resolve
     * @return the java class type of the given type (or {@code null} code if not defined)
     */
    public static Type getType(Context context, ObjectType type) {
        if(type instanceof Type) {
            return (Type)type;
        } else {
            TypeDefinition def=context.getTypeLoader().resolveType(type);
            return def==null?null:def.getJavaClassType();
        }
    }
    
    /**
     * Check and resolve if the given class is a network object in the provided namespace.
     * 
     * @param ns the namespace
     * @param clazz the class to check
     * @return the network object annotation (if such an object exist) for the given class and the given namespace 
     */
    public static NetworkObject getNetworkObject(Namespace ns,Class<?> clazz) {
        for(NetworkObject obj:clazz.getAnnotationsByType(NetworkObject.class)) {
            if(ns.getUrnPrefix().equals(obj.ns())) {
                return obj;
            }
        }
        return null;
    }
    
    /**
     * Check and resolve if the given method is a network object in the provided namespace.
     * 
     * @param ns the namespace
     * @param m the method to check
     * @return the network object annotation (if such an object exist) for the given method and the given namespace
     */
    public static NetworkObject getNetworkObject(Namespace ns,Method m) {
        for(NetworkObject obj:m.getAnnotationsByType(NetworkObject.class)) {
            if(ns.getUrnPrefix().equals(obj.ns())) {
                return obj;
            }
        }
        return null;
    }
}
