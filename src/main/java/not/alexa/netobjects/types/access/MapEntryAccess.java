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

import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.utils.Sequence;

/**
 * This is a special class implementing {@link Access}. The
 * type needs to be a class type with two entries. The first
 * entry is considered as the "key", the second as the "value".
 * The accessible object returned by {@link #newAccessible(AccessContext)}
 * implements the {@link Map.Entry} interface.
 * 
 * @author notalexa
 *
 */
public class MapEntryAccess extends Access.AbstractAccess implements Access {
	protected ClassTypeDefinition type;
	protected AccessFactory factory;
	protected Access[] fieldAccess;
	public MapEntryAccess(AccessFactory factory,ClassTypeDefinition type,Access[] fieldAccess) {
		this.type=type;
		this.factory=factory;
		this.fieldAccess=fieldAccess;
	}

    @Override
    public AccessFactory getFactory() {
        return factory;
    }

	@Override
	public TypeDefinition getType() {
		return type;
	}

	@Override
	public AccessibleObject newAccessible(AccessContext context) throws BaseException {
		return new Instance();
	}
	
	@Override
	public Field[] getFields() {
		return type.getFields();
	}

	@Override
	public Object getField(AccessContext context,Object o, Field f) throws BaseException {
		Map.Entry<?,?> entry=(Map.Entry<?,?>)o;
		switch(f.getIndex()) {
			case 0:return entry.getKey();
			case 1:return entry.getValue();
		}
		return super.getField(context,o, f);
	}

	@Override
	public void setField(AccessContext context,Object o, Field f, Object v) throws BaseException {
		switch(f.getIndex()) {
			case 0:if(o instanceof Instance) {
					((Instance)v).key=v;
				} else {
					throw new BaseException();
				}
				break;
			case 1:((Map.Entry<Object,Object>)o).setValue(v);
				break;
		}
	}

	@Override
	public Access getFieldAccess(Field f) throws BaseException {
		return fieldAccess[f.getIndex()];
	}

	private class Instance implements AccessibleObject,Sequence<AccessibleObject>,Map.Entry<Object,Object> {
		Object key;
		Object value;
		
		@Override
		public TypeDefinition getType() {
			return type;
		}

		@Override
		public Object getObject() {
			return this;
		}

		@Override
		public Object getKey() {
			return key;
		}
		
		@Override
		public Object getValue() {
			return value;
		}


		@Override
		public Object setValue(Object arg0) {
			Object old=getValue();
			value=arg0;
			return old;
		}

		@Override
		public void setField(AccessContext context,Field f, AccessibleObject value) throws BaseException {
			Access fieldAccess=getFieldAccess(f);
			switch(f.getIndex()) {
				case 0:this.key=fieldAccess.getObject(context,value); break;
				case 1:this.value=fieldAccess.getObject(context,value); break;
			}
		}

		@Override
		public AccessibleObject getField(AccessContext context,Field f) throws BaseException {
			Access fieldAccess=getFieldAccess(f);
			switch(f.getIndex()) {
				case 0:return fieldAccess.makeAccessible(context,key);
				case 1:return fieldAccess.makeAccessible(context,value);
			}
			return null;
		}

		@Override
		public boolean busy() {
			return true;
		}

		@Override
		public AccessibleObject current() {
			return this;
		}

		@Override
		public Sequence<AccessibleObject> next() {
			return Sequence.emptySequence();
		}

		@Override
		public Sequence<AccessibleObject> asSequence() {
			return this;
		}
	}
}
