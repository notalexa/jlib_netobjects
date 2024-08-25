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

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.EnumTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;

/**
 * If a type is known but not linked locally to a Java class, it can be inserted into an object if the
 * member is either an object or an interface (using a proxy). The object can be deserialized in a generic
 * form and the objects can be reused using the {@link AccessibleObject} framework.
 * <br>Typical examples for situations of this type are proxies and callbacks.
 * <p>We have to consider classes and enumerations. This is the base class for the two types.
 * 
 * @author notalexa
 * 
 * @see DeferredClassAccess
 * @see DeferredEnumAccess
 */
public abstract class DeferredAccess implements Access,Cloneable {
	protected AccessFactory factory;
	private TypeDefinition type;
	public DeferredAccess(AccessFactory factory,TypeDefinition type) {
		this.factory=factory;
		this.type=type;
	}
	
	private static DeferredAccess createAccess(AccessFactory factory,TypeDefinition type) {
		switch(type.getFlavour()) {
			case ClassType: return new DeferredClassAccess(factory,(ClassTypeDefinition)type);
			case EnumType: return new DeferredEnumAccess(factory,(EnumTypeDefinition)type);
			default: return null;
		}
	}
	
	static Access get(AccessFactory factory,TypeDefinition type) {
		switch(type.getFlavour()) {
		case EnumType:
		case ClassType:
			DeferredAccess access=type.getAdapter(DeferredAccess.class);
			if(access==null) {
				synchronized (type) {
					access=type.getAdapter(DeferredAccess.class);
					if(access==null) {
	    	    				type.putAdapter(access=createAccess(factory,type));
					}
				}
			}
			return access.forFactory(factory);
		default:
			return null;
		}
	}

	@Override
	public AccessFactory getFactory() {
		return factory;
	}

	@Override
	public TypeDefinition getType() {
		return type;
	}
	
	public DeferredAccess forFactory(AccessFactory factory) {
		if(factory==this.factory) {
			return this;
		} else try {
			DeferredAccess access=(DeferredAccess)clone();
			access.factory=factory;
			return access;
		} catch(Throwable t) {
			return null;
		}
	}

	@Override
	public Object getObject(AccessibleObject o) throws BaseException {
		return o==null?null:o.getAssignable();
	}
}
