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
package not.alexa.netobjects.api;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.ObjectType;

/**
 * Indicates that the annotated element is a network object. Network objects can be
 * classes (the network object is of type {@link Flavour#ClassType}) or a method (the
 * network object is of type {@link Flavour#MethodType}.
 * <br>The annotation indicates the {@link ObjectType}(s) of a given object. Therefore
 * the namespace (via {@link #ns()}), and the name (via {@link #base()} and {@link #id()})
 * should be constructable.
 * <br>The default value for the namespace is {@code jvm} representing the Java Namespace. For
 * classes, the value is fixed and should represent the class itself. Therefore explicit values
 * for {@link #id()} and {@link #base()} are forbidden. For methods, the canonical base is the 
 * class or interface for which the method is constructed (this is <em>not</em> the class defining
 * the method itself). For example if a class {@code B extends A} and {@link A} defines a method
 * declared as a network object via this annotation and the class definition of {@link B} is
 * constructed than the method belongs to {@link B} since class inheritance is not defined
 * for network objects.
 *  
 * @author notalexa
 *
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE,METHOD})
@Repeatable(NetworkObjects.class)
public @interface NetworkObject {
    /**
     * The return value defaults to {@code jvm}.
     * 
     * @return the namespace of this network object
     */
    public String ns() default "jvm";
    /**
     * A base class for this network object. The meaning depends on the location of
     * the annotation and the resolution mechanism defined in the given namespace. For
     * the default namespace the base class should <b>not</b> be given and defaults to
     * <em>no base class<em> for classes and <em>the resolving class</em> for methods.
     * 
     * <br>For an XML namespace,
     * the base class may refer to a class which defines the global schema.
     * 
     * @return the base class associated to this network object
     */
    public Class<?> base() default Object.class;
    
    /**
     * The id of this network object. The meaning depends on the location of the annotation
     * and the resolution mechanims defined in the given namespace. For the default namespace
     * the id <b>must</b> be omitted in case of classes and <b>may</b> omitted in case of
     * methods overriding the default mechanism in constructing the "method name" in the
     * java class type.
     * 
     * @return the id of the network object
     */
    public String id() default "##id";
}
