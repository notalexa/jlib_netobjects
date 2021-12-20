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
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import not.alexa.netobjects.utils.TypeUtils;

/***
 * Declares a network type as abstract. "Abstract" in this context means that any
 * reference of the type (like fields) may be replaced by a "derived" type. Derivation is
 * not perfectly defined for network types but after linkage the (Java) class typically
 * extends the abstract type (or implements the interface if the abstract type is an interface).
 * <br>For network types, any derived type <b>must have at least the same fields and methods
 * as the base (abstract) type</b>.
 * <br>Being abstract should not be checked directly by retrieving the annotation. Instead
 * the method {@link TypeUtils#isAbstract(Class)} should be used since interfaces and abstract
 * classes are automatically abstract in the network sense too.
 * <p>In contrast to object orientated languages, fields on network objects are 
 * assumed to match the type definition exactly by default if the type is not abstract. To
 * allow better granularity, fields in network objects can be declared as abstract too. In this
 * case any (objects of) any derived class can be the value of the field even if the class represents a different
 * network type. Therefore abstract fields are closer to typical fields in an object orientated
 * language (but non abstract fields are more efficient and closer to typical coding
 * conventions).
 * 
 * @author notalexa
 * @see TypeUtils#isAbstract(Class)
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE,FIELD,METHOD,TYPE_USE})
public @interface Abstract {
}
