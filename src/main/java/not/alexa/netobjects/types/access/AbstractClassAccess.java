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
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Constructor.PreAccessible;

/**
 * Abstract access implementation useful for network objects implementing it's own explicit
 * access. Extensions should either
 * <ul>
 * <li>Define a constructor with arguments {@link AccessFactory} <b>and</b> {@link Constructor}
 * to use the default constructor attached to the class.
 * <li>Define a constructor with argument {@link AccessFactory} and override the {@link #newInstance(AccessContext)}
 * or {@link #newAccessible(AccessContext)} method to define it's own object creation.
 * </ul>
 * 
 * @author notalexa
 *
 */
public abstract class AbstractClassAccess implements Access {
    protected Constructor constructor;
	protected ClassTypeDefinition classType;
	protected Access[] fieldAccess;
	protected AccessFactory factory;
    public AbstractClassAccess(AccessFactory factory,ClassTypeDefinition classType,Constructor constructor) {
        this(factory,classType);
        this.constructor=constructor;
    }
	public AbstractClassAccess(AccessFactory factory,ClassTypeDefinition classType) {
		this.classType=classType;
		this.factory=factory;
		fieldAccess=new Access[classType.getFields().length];
	}
	
	@Override
	public AccessFactory getFactory() {
	    return factory;
	}

	@Override
	public ClassTypeDefinition getType() {
		return classType;
	}
	
	@Override
    public AccessibleObject newAccessible(AccessContext context) throws BaseException {
	    return newInstance(context).makeAccessible(this);
    }
	
	/**
	 * Create a new object for this type.
	 * 
	 * @param context the context to use for creation
	 * @return an instance of the global type
	 * @throws BaseException if an error occurs
	 */
	protected PreAccessible newInstance(AccessContext context) throws BaseException {
	    return constructor.newInstance(context);
	}

	@Override
	public Field[] getFields() {
		return classType.getFields();
	}

	@Override
	public Object getField(Object o, Field f) throws BaseException {
		return getField(o,f.getIndex());
	}
	
	protected abstract Object getField(Object o,int index) throws BaseException;

	@Override
	public void setField(Object o, Field f, Object v) throws BaseException {
		setField(o,f.getIndex(),v);
	}
	
	protected abstract void setField(Object o,int index,Object v) throws BaseException;

	@Override
	public Access getFieldAccess(Field f) throws BaseException {
		Access access=fieldAccess[f.getIndex()];
		if(access==null) {
			synchronized (this) {
				access=fieldAccess[f.getIndex()];
				if(access==null) {
					access=fieldAccess[f.getIndex()]=createFieldAccess(f);
				}
			}
		}
		return access;
	}
	
	protected Access createFieldAccess(Field f) throws BaseException {
		return factory.resolve(this,f.getType());
	}
	
	protected Access forArray(TypeDefinition description,Class<?> clazz) {
		if(clazz.isArray()) {
			return new ArrayTypeAccess(description, forArray(((ArrayTypeDefinition)description).getComponentType(),clazz.getComponentType()),clazz);
		} else {
			return factory.resolve(this,description);
		}
	}

	protected Access forCollection(TypeDefinition description,Class<?> clazz) {
	    return new ArrayTypeAccess(description, factory.resolve(this,((ArrayTypeDefinition)description).getComponentType()),clazz);
	}

	protected Access forMap(TypeDefinition description,Class<? extends Map> clazz) {
		return new ArrayTypeAccess(description,new MapEntryAccess(factory,this,(ClassTypeDefinition)((ArrayTypeDefinition)description).getComponentType()), clazz);
	}
}
