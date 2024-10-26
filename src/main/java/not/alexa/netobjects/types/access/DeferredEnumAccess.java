
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

import java.util.HashSet;
import java.util.Set;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.EnumTypeDefinition;
import not.alexa.netobjects.types.EnumTypeDefinition.Value;

/**
 * Access to deferred enumeration objects.
 * 
 * @author notalexa
 */
public class DeferredEnumAccess extends DeferredAccess {
	private Set<String> values=new HashSet<String>();
	public DeferredEnumAccess(AccessFactory factory, EnumTypeDefinition enumType) {
		super(factory, enumType);
		for(Value v:enumType.getValues()) {
			values.add(v.getEnumValue());
		}
	}

	@Override
	public AccessibleObject newAccessible(AccessContext context) throws BaseException {
		throw new BaseException();
	}

	@Override
	public AccessibleObject makeAccessible(AccessContext context,Object o) throws BaseException {
		if(values.contains(o)) {
			return new DefaultAccessibleObject(this, o);
		} else {
			throw new BaseException(BaseException.FORBIDDEN,"Not an enum value: "+o);
		}
	}
}
