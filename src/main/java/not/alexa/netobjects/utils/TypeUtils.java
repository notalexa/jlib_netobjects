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
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Abstract;
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
            if(isFinal(clazz)) {
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
    
    public static ClassResolver createClassResolver(Class<?> type) {
        return new ClassResolver(type);
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
    
    /**
     * Class Resolver for a given class. Types referenced in the class can be resolved (that is type variables are replaced by there type for example)
     * using {@link #resolve(AnnotatedType)}. Outcome is always a {@link ResolvedClass} with a Java class as main type and parameters (empty if no
     * parameteres are present) of type {@link ResolvedClass}. Since {@code ResolvedClass} implements {@code AnnotatedElement} annotations for parameter
     * types can be resolved via the outcome.
     * 
     * @author notalexa
     *
     */
    public static final class ClassResolver {
        private Class<?> clazz;
        private Map<java.lang.reflect.Type,AnnotatedType> resolvedVariables=new HashMap<>();
        ClassResolver(Class<?> clazz) {
            this.clazz=clazz;
            init(clazz);
        }
       
        /**
         * 
         * @return the root class of this resolver
         */
        public Class<?> getRootClass() {
            return clazz;
        }
        
        private Class<?> resolve(AnnotatedParameterizedType type) {
            java.lang.reflect.Type rawType=((ParameterizedType)type.getType()).getRawType();
            AnnotatedType[] resolved=type.getAnnotatedActualTypeArguments();
            if(rawType instanceof Class) {
                Class<?> clazz=(Class<?>)rawType;
                TypeVariable<?>[] typeVars=clazz.getTypeParameters();
                if(typeVars.length==resolved.length) {
                    for(int i=0;i<typeVars.length;i++) {
                        resolvedVariables.put(typeVars[i],resolved[i]);
                    }
                }
                return clazz;
            }
            return null;
        }
        
        private void init(java.lang.reflect.Type cl) {
            if(cl!=null) {
                if(cl instanceof Class) {
                    Class<?> clazz=(Class<?>)cl;
                    init(clazz.getAnnotatedSuperclass());
                    for(AnnotatedType type:clazz.getAnnotatedInterfaces()) {
                        init(type);
                    }
                }
            }
        }

        private void init(AnnotatedType cl) {
            if(cl!=null) {
                if(cl instanceof AnnotatedParameterizedType) {
                    init(resolve((AnnotatedParameterizedType)cl));
                } else {
                    init(cl.getType());
                }
            }
        }

        /**
         * Resolve the (annotated) type. 
         * 
         * @param annotatedType The annotated type to resolve
         * @return an object with the
         * <ul>
         * <li>The type resolved to a class
         * <li>Annotations attatched to the type
         * <li>Resolved parameter types (with annotations attached)
         * </ul>
         * @throws RuntimeException if the type cannot be completely resolved (including the parameter types).
         * This can happen if the root class has parameters for example (or the type is a parameter or argument of
         * a method of the class with individual type variable).
         * 
         */
        public ResolvedClass resolve(AnnotatedType annotatedType) {
            AnnotatedType type=resolvedVariables.getOrDefault(annotatedType.getType(),annotatedType);
            if(type instanceof AnnotatedTypeVariable) {
                throw new RuntimeException("Unresolved type variable: "+type);
            } else if(type instanceof AnnotatedParameterizedType) {
                AnnotatedParameterizedType p=(AnnotatedParameterizedType)type;
                java.lang.reflect.Type rawType=((ParameterizedType)p.getType()).getRawType();
                AnnotatedType[] parameters=p.getAnnotatedActualTypeArguments();
                ResolvedClass[] resolvedParameters=new ResolvedClass[parameters.length];
                for(int i=0;i<parameters.length;i++) {
                    resolvedParameters[i]=resolve(parameters[i]);
                }
                if(rawType instanceof Class) {
                    return new ResolvedClass((Class<?>)rawType,annotatedType,resolvedParameters);
                }
            } else if(type instanceof AnnotatedArrayType) {
                AnnotatedArrayType array=(AnnotatedArrayType)type;
                return new ResolvedClass(Object[].class,new ResolvedClass[] { resolve(array.getAnnotatedGenericComponentType())});
            } else if(type instanceof AnnotatedWildcardType) {
                AnnotatedWildcardType wildcardType=(AnnotatedWildcardType)type;
                AnnotatedType[] bounds=wildcardType.getAnnotatedUpperBounds();
                if(bounds.length!=1) {
                    throw new RuntimeException("Unsupported multiple bounds");
                }
                return resolve(bounds[0]);
            } else if(type.getType() instanceof Class) {
                Class<?> clazz=(Class<?>)type.getType();
                if(clazz.isArray()) {
                    return new ResolvedClass(Object[].class,annotatedType,new ResolvedClass[] { resolve(new AnnotatedType() {
                        @Override
                        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                            return type.getAnnotation(annotationClass);
                        }

                        @Override
                        public Annotation[] getAnnotations() {
                            return type.getAnnotations();
                        }

                        @Override
                        public Annotation[] getDeclaredAnnotations() {
                            return type.getDeclaredAnnotations();
                        }

                        @Override
                        public java.lang.reflect.Type getType() {
                            return clazz.getComponentType();
                        }
                    })});
                } else {
                    return new ResolvedClass((Class<?>)type.getType(),annotatedType,null);
                }
            }
            throw new RuntimeException("Unresolved type: "+type);
        }
        
        private ResolvedClass resolve(AnnotatedType current,Class<?> clazz) {
            ResolvedClass resolved=resolve(current);
            Class<?> c=resolved.getResolvedClass();
            if(clazz.equals(c)) {
                return resolved;
            } else {
                return resolveHierachy(c, clazz);
            }
        }
            
        private ResolvedClass resolveHierachy(Class<?> c,Class<?> clazz) {
            for(AnnotatedType type:c.getAnnotatedInterfaces()) {
                ResolvedClass resolved=resolve(type,clazz);
                if(resolved!=null) {
                    return resolved;
                }
            }
            return c.getAnnotatedInterfaces()==null?null:resolve(c.getAnnotatedSuperclass(),clazz);                
        }
        
        /**
         * Resolve the given class as a class in the class hierarchy of the root class.
         * @param clazz the class to resolve
         * @return the resolved class or {@code null} if the class is not resolvable (in case the class is not contained in the root class hierachy for example) 
         */
        public ResolvedClass resolve(Class<?> clazz) {
            if(clazz.equals(getRootClass())) {
                return new ResolvedClass(getRootClass(),new ResolvedClass[0]);
            } else {
                return resolveHierachy(getRootClass(), clazz);
            }
        }
    }
    
    /**
     * Resolved class for a given type. This class and it's constructors are public since type resolvers may modify the
     * outcome of class reflection (for example {@code ByteArray} or {@code Optional} may be resolved to the byte array or
     * parameter type introducing additional annotations to reflect the optional character of the field.
     * <br>Arrays are a little bit special. In this case the resolved class is <b>always {@code Object[].class} and the
     * parameter represents the component type</b>. Consistent with the mapping and representation of arrays a class is
     * mapped to an array if
     * <ul>
     * <li>It is an array
     * <li>Implements (or extends) the {@code Collection} interface.
     * <li>Implements (or extends) the {@code Map} interface.
     * </ul>
     * 
     * @author notalexa
     *
     */
    public static class ResolvedClass implements AnnotatedElement {
        private static final ResolvedClass[] NO_PARAMETERS=new ResolvedClass[0];
        private Class<?> clazz;
        private ResolvedClass[] parameters;
        private AnnotatedElement annotationSource;
        
        /**
         * 
         * @param clazz the resolved type (also used as the source of annotations)
         * @param parameters the parameters of the class
         */
        public ResolvedClass(Class<?> clazz, ResolvedClass[] parameters) {
            this(clazz,clazz,parameters);
        }
        
        /**
         * 
         * @param clazz the resolved type
         * @param annotationHolder the annotation source of the resolved class
         * @param parameters the parameters of the resolved type
         */
        public ResolvedClass(Class<?> clazz, AnnotatedElement annotationSource, ResolvedClass[] parameters) {
            this.clazz=clazz;
            this.annotationSource=annotationSource;
            this.parameters=parameters==null?NO_PARAMETERS:parameters;
        }
        
        /**
         * @return The resolved class of the type
         */
        public Class<?> getResolvedClass() {
            return clazz;
        }
        
        /**
         * 
         * @return {@code true} if this class has parameters
         */
        public boolean hasParameters() {
            return parameters.length>0;
        }
        
        /**
         * 
         * @return the parameters of the resolved type
         */
        public ResolvedClass[] getParameters() {
            return parameters;
        }
        
        public String toString() {
            if(parameters.length>0) {
                StringBuilder s=new StringBuilder(clazz.getName()).append('<').append(parameters[0].toString());
                for(int i=1;i<parameters.length;i++) {
                    s.append(',').append(parameters[i]);
                }
                return s.append('>').toString();
            } else {
                return clazz.getName();
            }
        }
        
        /**
         * @return {@code true} if the resolved class represents an array. This is true if the class itself is an
         * array or a collection or a map.
         */
        public boolean isArray() {
            return clazz.isArray()||Map.class.isAssignableFrom(clazz)||Collection.class.isAssignableFrom(clazz);
        }
        
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return annotationSource.getAnnotation(annotationClass);
        }
        @Override
        public Annotation[] getAnnotations() {
            return annotationSource.getAnnotations();
        }
        
        @Override
        public Annotation[] getDeclaredAnnotations() {
            return annotationSource.getDeclaredAnnotations();
        }
    }
    
    /**
     * Is the class final (as a network object)? For final network types overlays are
     * forbidden (but extensions are allowed).
     * <br>A class is final if it is final as a java class (since in this case it cannot
     * be overridden) or marked as final using the {@link Final} annotation
     * @param clazz the class to check
     * @return {@code true} if the class is final
     * @see Final
     */
    public static boolean isFinal(Class<?> clazz) {
        return Modifier.isFinal(clazz.getModifiers())||clazz.getAnnotation(Final.class)!=null;
    }
    
    /**
     * Is this class abstract (as a network object)? Abstract classes can be overridden with
     * different (network) types. Therefore, additional type information must be included for
     * abstract types.
     * <br>Abstract types are interfaces and classes which are either declared as abstract (in the java
     * sense) or marked as abstract using the {@link Abstract} annotation.
     * <br>Typically abstract classes are avoided if possible since extra information is needed
     * (and breaks the "character" of serialized objects). The framework introduce the
     * notation of "abstract" fields which allows the architect to choose abstraction with
     * a better granularity. Coding schemes typically generates additional type information if either
     * the field is abstract or the type of the field is abstract (or both).
     * 
     * @param clazz the class to check
     * @return {@code true} code if the class is abstract
     */
    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers())||clazz.getAnnotation(Abstract.class)!=null;
    }
}
