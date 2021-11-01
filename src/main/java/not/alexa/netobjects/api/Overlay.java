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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import not.alexa.netobjects.types.TypeLoader;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessFactory;

/**
 * Structured data like JSON or XML files define equivalence relations in a natural way: 
 * Two data objects are in the same class if the possible attribute set is the same. In
 * this way there exist for example exactly one class with <i>no attributes</i>. Obviously 
 * this relation is not very useful since objects may differ in there meaning even if they
 * have no attributes. Serialized Java Objects for example define the class itself 
 * as part of the relation (which means that the attribute set is always the same).
 * 
 * Network objects define the global type as part of the relation. Two objects are the
 * same if the type definition contains the object type of both objects. Nothing is said
 * about how to link this to a special Java class. In general, the object type is chosen
 * as a Java class type and the <b>natural</b> binding is to link this to the corresponding
 * Java class. Overlays weaken this relation and are central in linking network objects to
 * Java classes. Roughly an Overlay of class <code>A</code> is an inherited class <code>B</code>
 * which has no additional global attributes. Overlays are marked with this annotation and
 * also denotes the class the override with the following conventions:
 * 
 * <ul>
 * <li>The value of this annotation is a superclass of the annoted class
 * <li>The annoted class has the same global attributes as the value of this annotation
 * <li>The default value of this annotation is <code>Object</code> which marks by
 * convention the super class of the annotated class.
 * </ul>
 * 
 * Typically overlays are related to the linkage process and therefore should be integrated
 * using the {@link Access} and {@link AccessFactory} interfaces. Since these are deeply
 * related to coding schemes and coding schemes are heavy weighted (both in creation and
 * usage), the API wants to avoid too many {@link AccessFactory} instances. Therefore,
 * overlays are related to {@link TypeLoader} instances using the {@link TypeLoader#overlay(Class...)}
 * or {@link TypeLoader#overlay(java.util.Set)} methods.
 * 
 * @author notalexa
 * @see TypeLoader#overlay(Class...)
 * @see TypeLoader#overlay(java.util.Collection)
 * @see TypeLoader#getLinkedLocal(not.alexa.netobjects.types.TypeDefinition)
 * 
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Overlay {
    public Class<?> value() default Object.class;
}
