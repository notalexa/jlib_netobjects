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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Basic annotation to denote the name of a field. A field can be specified on a per type
 * base.
 * <br>Fields can be defined on a per type (that is serialization type like XML,...) base.
 * For example, the annotation {@code Field(value=80,type='protobuf')} will set the index of the annotated field
 * in the protobuf serialization to {@code 80} (and the field name to the default value) while
 * {@code Field(80)} set's the overall index to 80 (including proto buffers).
 * 
 * @author notalexa
 * 
 * @see not.alexa.netobjects.types.ClassTypeDefinition.Field#getTag(String...)
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE_USE})
@Repeatable(Fields.class)
public @interface Field {
	/**
	 * 
	 * @return the index of the field
	 */
    public int value() default -1;
    /**
     * Typical types are {@code xml}, {@code json}, {@code yaml}.
     * 
     * @return the type for the 
     */
    public String type() default "*";
    
    /*
     * 
     */
    public String name() default "";
}
