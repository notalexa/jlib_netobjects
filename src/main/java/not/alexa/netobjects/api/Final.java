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

import not.alexa.netobjects.utils.TypeUtils;

/**
 * Opposite to {@link Overlay}. Classes annotated as final cannot be overlayed and
 * should be considered as structured data only. Use of this annotation should not
 * be checked directly but {@link TypeUtils#isFinal(Class)} should be used (since 
 * classes which are declared as finally cannot be overridden and therefore not overlayed).
 * 
 * @author notalexa
 * @see TypeUtils#isFinal(Class)
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Final {

}
