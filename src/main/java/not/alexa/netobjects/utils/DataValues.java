/*
 * Copyright (C) 2023 Not Alexa
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

import java.util.Map;

/**
 * Simple interface wrapping values. Typically, an object of this type is added to a context to add additional values while deserializing
 * content using
 * <pre>
 * context.putAdapter(DataValues.fromMap(...))
 * scheme.createDecoder(context,...).decode(Object.class)
 * </pre>
 * 
 * @author notalexa
 * @see
 */
public interface DataValues {
	/**
	 * Get the value for the given key.
	 * @param key the key
	 * @return the value for the key or {@code null} if not present
	 */
	public Object get(Object key);
	
	/**
	 * @param values the map which should be wrapped
	 * @return a {@link DataValues} object wrapping the {@code values}
	 */
	public static DataValues fromMap(Map<?,?> values) {
		return new DataValues() {
			@Override
			public Object get(Object key) {
				return values.get(key);
			}
		};
	}
}
