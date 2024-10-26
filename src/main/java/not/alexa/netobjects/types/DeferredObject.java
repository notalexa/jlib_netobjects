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
package not.alexa.netobjects.types;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Castable;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;
import not.alexa.netobjects.utils.InvocationSupport;

/**
 * Class representing an object with "buildin" resolution. Objects of this type are serialized as
 * interfaces without type information.
 * 
 * @author notalexa
 */
public class DeferredObject implements InvocationSupport, Deferred<DeferredObject,Object>, Castable {
	protected Object o;

	public static ClassTypeDefinition getTypeDescription() {
		return Types.DEFERRED_TYPE;
	}
	
	protected DeferredObject() {
	}

	public DeferredObject(Object o) {
		if(o==null) {
			throw new NullPointerException();
		}
		this.o=o;
	}
	
	public Object getObject() {
		return o;
	}
	
	public Object getCodingObject(AccessContext context) {
		return getObject();
	}
	
	public void setObject(Object o) {
		if(o==null) {
			throw new NullPointerException();
		}
		this.o=o;
	}
	
	public <T> T get(Context context,Class<T> clazz) throws BaseException {
		return get(clazz);
	}
	
	public <T> T get(Class<T> clazz) throws BaseException {
		if(clazz.isInstance(o)) {
			return (T)o;
		}
		return null;
	}
	
	@Override
	public boolean isResolved() {
		return true;
	}

	public static class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory) {
			super(factory,getTypeDescription());
		}
		
		@Override
		public AccessibleObject newAccessible(AccessContext context) throws BaseException {
			return new DefaultAccessibleObject(this, new DeferredObject());
		}

		@Override
		protected Object getField(AccessContext context,Object o, int index) throws BaseException {
			return null;
		}

		@Override
		protected void setField(AccessContext context,Object o, int index, Object v) throws BaseException {
		}
	}
	
	protected Class<?>[] getProxyClasses(Class<?> clazz) {
		return new Class[] { clazz, Deferred.class };
	}


	public final Object makeProxy(Access tagAccess) throws BaseException {
		Class<?> clazz=tagAccess.getType().asClass(tagAccess.getAccessLoader());
		if(clazz.isInterface()) {
			return makeProxy(clazz);
		} else if(clazz.equals(Object.class)) {
			return this;			
		} else {
			throw new BaseException();
		}
	}
	
	@Override
	public Object makeProxy(Class<?> clazz) {
		return Proxy.newProxyInstance(clazz.getClassLoader(), getProxyClasses(clazz), this);
	}

	@Override
	public ObjectType getObjectType(Namespace ns) {
		return o==null?null:ObjectType.createClassType(o.getClass());
	}
	
	@Override
	public Object resolveObject(Context context,Method m) {
		Class<?> clazz=m.getDeclaringClass();
		if(clazz.isInstance(o)) {
			return o;
		} else if(!isResolved()) {
			Type t=(Type)getObjectType(JavaClass.getJavaNamespace());
			LinkedLocal linkedLocal=t==null?null:t.asLinkedLocal(context.getTypeLoader().getClassLoader());
			if(linkedLocal!=null&&clazz.isAssignableFrom(linkedLocal.asClass())) try {
				return get(context,clazz);
			} catch(Throwable t0) {
				t0.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public <T> T castTo(Context context, Class<T> clazz) {
		if(clazz.isInstance(o)) {
			return (T)o;
		} else if(clazz.isInstance(this)) {
			return (T)this;
		} else if(!isResolved()) {
			Type t=(Type)getObjectType(JavaClass.getJavaNamespace());
			if(t!=null) {
				LinkedLocal linkedLocal=t.asLinkedLocal(context.getTypeLoader().getClassLoader());
				if(linkedLocal!=null&&clazz.isAssignableFrom(linkedLocal.asClass())) try {
					return get(context,clazz);
				} catch(Throwable t0) {
					t0.printStackTrace();
				}
				if(clazz.isInterface()) {
					return (T)Proxy.newProxyInstance(clazz.getClassLoader(), getProxyClasses(clazz), this);
				}
			}
		}
		return null;
	}

	@Override
	public Access getCodingAccess(AccessContext accessContext, AccessFactory factory) {
		if(o!=null) {
			Context context=accessContext.getContext();
			return factory.resolve(context, context.resolveType(o.getClass()));
		} else {
			return null;
		}
	}
}
