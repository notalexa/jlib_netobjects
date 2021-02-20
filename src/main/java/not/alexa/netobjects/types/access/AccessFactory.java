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
package not.alexa.netobjects.types.access;

import not.alexa.netobjects.Adaptable;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.TypeDefinition;

/**
 * Factory for creating access for a given type. Two versions exist:
 * The first one resolves for a given context, the second
 * one for a given referrer.
 * <p>In all cases where non generic classes are used, the
 * access <b>does not depend uniquely on the type definition</b>
 * but some class retrieval too. In most cases, this is the
 * primary class loader provided by the context in {@link #resolve(Context, TypeDefinition)}.
 * 
 * @author notalexa
 *
 */
public interface AccessFactory extends Adaptable {
	
	/**
	 * Resolve access for the given type and context. In general,
	 * the access depends on the class loader of the context.
	 * 
	 * @param context the context to use
	 * @param type the type
	 * @return access for the given type in the given context
	 */
	public default Access resolve(Context context,TypeDefinition type) {
		return type.getAdapter(Access.class);
	}
	
	/**
	 * Resolve access for the type and referrer. In general,
	 * the access depends on the class loader of the referrer.
	 * 
	 * @param referrer the referrer
	 * @param type the tpe
	 * @return access for the given type relative to the referrer
	 */
	public default Access resolve(Access referrer,TypeDefinition type) {
		return type.getAdapter(Access.class);
	}	
}
