/*
 * Copyright (C) 2024 Not Alexa
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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import not.alexa.netobjects.types.ClassTypeDefinition.Field;

/**
 * Coding hints are intended to express how coding should be performed if the
 * hint is respected. All hints are optional.
 * <br>An example for a coding hint is {@code inline}, which expressed the wish to
 * represent a map as an associative array in the message. The hint is respected in YAML and JSON
 * but not in XML.
 * 
 * @author notalexa
 * 
 * @see Field#hasHint(String)
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.FIELD,ElementType.PARAMETER})
@Repeatable(CodingHints.class)
public @interface CodingHint {
	/**
	 * @return the name of this hint
	 */
	public String value();
}
