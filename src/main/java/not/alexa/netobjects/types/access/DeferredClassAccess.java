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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Castable;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Deferred;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.JavaClass;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
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
	
	private Access resolve(TypeDefinition type) {
		if(type.getFlavour()==Flavour.ArrayType) {
			return new ArrayTypeAccess(type,resolve(((ArrayTypeDefinition)type).getComponentType()), List.class);
		} else {
			return factory.resolve(this, type);
		}
	}

	private Access[] fieldAccess() {
		if(fieldAccess==null) {
			synchronized (this) {
				if(fieldAccess==null) {
					Access[] a=new Access[getFields().length];
					for(Field f:getFields()) {
						a[f.getIndex()]=resolve(f.getType());
					}
					fieldAccess=a;
				}
			}
		}
		return fieldAccess;
	}

	@Override
	public AccessibleObject makeAccessible(AccessContext context,Object o) throws BaseException {
		if(o instanceof Serializer) {
			return ((Serializer<?>)o).asAccessible();
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
	public Object getField(AccessContext context,Object o, Field f) throws BaseException {
		try {
			return ((Serializer<?>)o).get(f.getIndex());
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
	}

	@Override
	public void setField(AccessContext context,Object o, Field f, Object v) throws BaseException {
		try {
			((Serializer<?>)o).set(f.getIndex(),v);
		} catch(Throwable t) {
			BaseException.throwException(t);
		}
	}

	@Override
	public Access getFieldAccess(Field f) throws BaseException {
		return fieldAccess()[f.getIndex()];
	}

	private static class Attributes extends AbstractAccessibleObject implements InvocationSupport, Serializer<Object>, Castable {
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
		public Object resolveObject(Context context,Method m) {
				Type t=(Type)getObjectType(JavaClass.getJavaNamespace());
				LinkedLocal linkedLocal=t==null?null:t.asLinkedLocal(context.getTypeLoader().getClassLoader());
				if(linkedLocal!=null&&m.getDeclaringClass().isAssignableFrom(linkedLocal.asClass())) try {
					AccessFactory factory=access.getFactory().forContext(context);
					Access cast=factory.resolve(context, getType());
					AccessContext accessContext=cast.createContext(context);
					AccessibleObject o=cast.newAccessible(accessContext);
					for(int i=0;i<fields.length;i++) {
						Object v=fields[i];
						if(v!=null) {
							AccessibleObject f=getField(accessContext, cast.getFields()[i]);
							o.setField(accessContext, cast.getFields()[i], f);
						}
					}
					return o.getAssignable(accessContext);
				} catch(Throwable t0) {
					t0.printStackTrace();
				}
			return null;
		}

		
		@Override
		public <T> T castTo(Context context, Class<T> clazz) {
			if(clazz.isInstance(this)) {
				return (T)this;
			} else {
				Type t=access.getType().getJavaClassType();
				LinkedLocal linkedLocal=t.asLinkedLocal(context.getTypeLoader().getClassLoader());
				if(linkedLocal!=null&&clazz.isAssignableFrom(linkedLocal.asClass())) try {
					AccessFactory factory=access.getFactory().forContext(context);
					Access cast=factory.resolve(context, getType());
					AccessContext accessContext=cast.createContext(context);
					AccessibleObject o=cast.newAccessible(accessContext);
					for(int i=0;i<fields.length;i++) {
						Object v=fields[i];
						if(v!=null) {
							AccessibleObject f=getField(accessContext, cast.getFields()[i]);
							o.setField(accessContext, cast.getFields()[i], f);
						}
					}
					return (T)o.getAssignable(accessContext);
				} catch(Throwable t0) {
					t0.printStackTrace();
				}  
				if(clazz.isInterface()) {
					return (T)makeProxy(clazz);
				} else {
					return null;
				}
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

		@Override
		public Object makeProxy(Class<?> clazz) {
			return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz, Serializer.class, Castable.class }, this);
		}

		@Override
		public <R> Object getCodingObject(AccessContext context) {
			return getObject();
		}

		@Override
		public boolean isResolved() {
			return true;
		}

		@Override
		public Access getCodingAccess(AccessContext context, AccessFactory factory) {
			return access;
		}
	}
	
	/**
	 * Interface locally used to intermediate between {@link Attributes} and proxies.
	 */
	public interface Serializer<T> extends Deferred<T,T> {
		public AccessibleObject asAccessible();
		public Object get(int index);
		public void set(int index,Object v);
	}

}
