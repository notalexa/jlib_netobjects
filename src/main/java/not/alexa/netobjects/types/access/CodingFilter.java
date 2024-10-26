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
package not.alexa.netobjects.types.access;

import java.io.File;

import not.alexa.netobjects.types.JavaClassTypeMapper;
import not.alexa.netobjects.types.access.FieldAccessor.Getter;
import not.alexa.netobjects.types.access.FieldAccessor.Setter;

/**
 * Extension of {@link JavaClassTypeMapper} to mediate between a (primitive) type and other "simple"
 * types like {@linkplain File}.
 *  
 * @author notalexa
 * @param <S> the source type, e.g. {@code File}.
 * @param <C> the coding type, e.g. {@code String}.
 */
public interface CodingFilter<S,C> extends JavaClassTypeMapper<S,C> {
	public default FieldAccessor filter(Class<?> primitiveClassType, FieldAccessor field) {
		if(primitiveClassType.equals(getCodingClass())) {
			return field.filter(this);
		} else {
			return field;
		}
	}

	@Override
	public default void install() {
		RuntimeInfo.addFilter(this);
	}

	/**
	 * Filter the original getter method.
	 * 
	 * @param getter the getter accessing the field
	 * @return a modified getter transforming from the coding type to the source type
	 */
	public Getter filter(Getter getter);
	
	/**
	 * Filter the original setter method.
	 * 
	 * @param setter the setter accessing the field
	 * @return a modified setter transforming from the source type to the coding type
	 */
	public Setter filter(Setter setter);
}
