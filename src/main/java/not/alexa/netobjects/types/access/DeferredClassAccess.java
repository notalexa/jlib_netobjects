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

import java.lang.reflect.Proxy;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Deferred;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.utils.InvocationSupport;

/**
 * Access to deferred objects with non class type definition.
 * The members are backed up in {@link Attributes}
 * 
 * @author notalexa
 */
public class DeferredClassAccess extends DeferredAccess {
	private ClassTypeDefinition classType;
	private Access[] fieldAccess;

	public DeferredClassAccess(AccessFactory factory, ClassTypeDefinition classType) {
		super(factory, classType);
		this.classType=classType;
	}

	private Access[] fieldAccess() {
		if(fieldAccess==null) {
			synchronized (this) {
				if(fieldAccess==null) {
					Access[] a=new Access[getFields().length];
					for(Field f:getFields()) {
						a[f.getIndex()]=factory.resolve(this, f.getType());
					}
					fieldAccess=a;
				}
			}
		}
		return fieldAccess;
	}

	@Override
	public AccessibleObject makeAccessible(Object o) throws BaseException {
		if(o instanceof Serializer) {
			return ((Serializer)o).asAccessible();
		}
		throw new BaseException();
	}
	

	@Override
	public AccessibleObject newAccessible(AccessContext context) throws BaseException {
		return new Attributes(this,fieldAccess().length);
	}

	@Override
	public Field[] getFields() {
		return classType.getFields();
	}

	@Override
	public Object getField(Object o, Field f) throws BaseException {
		try {
			return ((Serializer)o).get(f.getIndex());
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
	}

	@Override
	public void setField(Object o, Field f, Object v) throws BaseException {
		try {
			((Serializer)o).set(f.getIndex(),v);
		} catch(Throwable t) {
			BaseException.throwException(t);
		}
	}

	@Override
	public Access getFieldAccess(Field f) throws BaseException {
		return fieldAccess()[f.getIndex()];
	}

	private static class Attributes extends AbstractAccessibleObject implements InvocationSupport, Serializer {
		private Object[] fields;
		public Attributes(Access access,int size) {
			super(access);
			this.fields=new Object[size];
		}

		@Override
		public java.lang.Object getObject() {
			return this;
		}

		@Override
		public <T> T castTo(Context context, Class<T> clazz) {
			if(clazz.isInstance(this)) {
				return (T)this;
			} else if(clazz.isInterface()) {
				return (T)Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz, Serializer.class }, this);
			} else {
				return null;
			}
		}

		@Override
		public AccessibleObject asAccessible() {
			return this;
		}

		@Override
		public ObjectType getObjectType(Namespace ns) {
			return access.getType().getType(ns);
		}

		@Override
		public Object get(int index) {
			return fields[index];
		}

		@Override
		public void set(int index,Object v) {
			fields[index]=v;
		}
	}
	
	/**
	 * Interface locally used to intermediate between {@link Attributes} and proxies.
	 */
	public interface Serializer extends Deferred {
		public AccessibleObject asAccessible();
		public Object get(int index);
		public void set(int index,Object v);
	}

}
