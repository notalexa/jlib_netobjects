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
package not.alexa.netobjects.coding.yaml;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Castable;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.yaml.YamlEncoder.TokenHolder;
import not.alexa.netobjects.types.Deferred;
import not.alexa.netobjects.types.DeferredObject;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;

/**
 * Class representing a deferred object from a Json or YAML source.
 * 
 * @author notalexa
 */
class DeferredTokenizedObject extends DeferredObject implements TokenHolder {
	private ObjectType type;
	private Context context;
	private YamlCodingScheme scheme;
	private Token t;
	
	public DeferredTokenizedObject(Object o) {
		super(o);
	}

	public DeferredTokenizedObject(DeferredObject o) {
		super(o.getObject());
	}

	public DeferredTokenizedObject(Context context,YamlCodingScheme scheme,ObjectType type,Token t) {
		this.context=context;
		this.scheme=scheme;
		this.type=type;
		this.t=t;
	}

	@Override
	public <T> T get(Class<T> clazz) throws BaseException {
		return get(context,clazz);
	}

	@Override
	protected Class<?>[] getProxyClasses(Class<?> clazz) {
		return new Class[] { clazz, TokenHolder.class, Deferred.class, Castable.class };
	}
	
	public void setObject(Object o) {
		super.setObject(o);
		t=null;
		context=null;
		scheme=null;
	}
	
	@Override
	public ObjectType getObjectType(Namespace ns) {
		if(t!=null) {
			return type==null?null:ns.equals(type.getNamespace())?type:null;
		} else {
			return super.getObjectType(ns);
		}
	}


	@Override
	public <T> T get(Context context,Class<T> clazz) throws BaseException {
		T t=super.get(clazz);
		if(t==null&&this.t!=null) try {
			TypeDefinition def=type!=null?context.resolveType(type):context.resolveType(clazz);
			Class<?> typeClass=def.getJavaClassType()==null?null:def.getJavaClassType().asLinkedLocal(context.getTypeLoader().getClassLoader()).asClass();
			if(typeClass!=null&&clazz.isAssignableFrom(typeClass)) {
				t=scheme.newBuilder().setRootType(def).build().createDecoder(context, this.t).decode(clazz);
			}
			if(t!=null&&context==this.context) {
				setObject(t);
			}
		} catch(YamlException e) {
			return BaseException.throwException(e);
		}
		return t;
	}

	@Override
	public boolean isResolved() {
		return t==null;
	}
//
//	@Override
//	public CodingScheme getCodingScheme() {
//		return scheme;
//	}
//	
//	@Override
//	public Context getContext() {
//		return context;
//	}

	@Override
	public Token getToken() {
		return t;
	}
}
