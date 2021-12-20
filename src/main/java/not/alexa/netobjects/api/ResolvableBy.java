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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Most languages already knows about other frameworks for serialization and deserialization
 * and a lot of them uses annotation schemes to declare field mappings. Examples are 
 * <a href="https://www.oracle.com/technical-resources/articles/javase/jaxb.html">JAXB</a>
 * or the <a href="https://github.com/FasterXML/jackson">Jackson library</a>.
 * <br>One of the design goals of the network objects library is to include objects annotated by this
 * frameworks as own objects making transitions easier.
 * <p>Consider the case where class {@code A} is defined in some library and designed to be
 * serializable using the Jackson library. We want to integrate {@code A} into our library
 * and want to define some additional (network) methods which can be evaluated remotely. The
 * type definition in this case is based on conventions used in the Jackson library and can
 * be marked with this annotation as follows:
 * <pre>
 * public class B extends {@literal @ResolvableBy}("jackson") A {
 *   {@literal @NetworkObject}
 *   public String method(Context context,...) throws BaseException {
 *     ...
 *   }
 *   ...
 * }
 * </pre>
 * The Jackson Resolver defined in a separate package recognizes the annotation and resolves
 * the fields of {@code A} using the conventions of the Jackson library defining a network
 * object {@code B} usable in this framework.
 * 
 * @author notalexa
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE,ElementType.TYPE_USE})
public @interface ResolvableBy {
    public String value();
}
