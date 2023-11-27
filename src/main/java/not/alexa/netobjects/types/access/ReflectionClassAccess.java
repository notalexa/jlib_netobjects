/*
 * Copyright (C) 2022 Not Alexa
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
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.utils.TypeUtils;
import not.alexa.netobjects.utils.TypeUtils.ClassResolver;
import not.alexa.netobjects.utils.TypeUtils.ResolvedClass;

/**
 * Standard class access using reflection ({@link NField}. The field is resolved as follows:
 * Beginning at the class, the field is resolved using the {@code getDeclaredField} method on
 * {@code java.lang.reflect.Field}.
 * 
 * @author notalexa
 *
 */
class ReflectionClassAccess extends AbstractClassAccess {
    private NField[] fields;
    ReflectionClassAccess(AccessFactory factory, Class<?> clazz,ClassTypeDefinition classType, Constructor constructor) {
        super(factory, classType, constructor);
        Field[] classFields=classType.getFields();
        fields=new NField[classFields.length];
        for(int i=0;i<fields.length;i++) {
            fields[i]=createFieldAccess(new Resolver(clazz),clazz,classFields[i]);
        }
    }
    
    protected NField createFieldAccess(Resolver resolver,Class<?> clazz,Field f) {
        if(clazz!=null) {
            try {
                java.lang.reflect.Field classField=clazz.getDeclaredField(constructor.mapField(clazz,f.getName()));
                classField.setAccessible(true);
                return new NField.FieldImpl(resolver.resolve(classField),classField);
            } catch(Throwable t) {
                
            }
            return createFieldAccess(resolver,clazz.getSuperclass(),f);
        } else {
            return new NField.UnknownField(resolver.getName(),f.getName());
        }
    }

    @Override
    protected Object getField(Object o, int index) throws BaseException {
        return fields[index].get(o);
    }

    @Override
    protected void setField(Object o, int index, Object v) throws BaseException {
        fields[index].set(o, v);
    }
    
    @Override
    public Access createFieldAccess(Field f) throws BaseException {
    	return createAccess(f.getType(),fields[f.getIndex()].getFieldType());
    }
    
    private class Resolver {
    	Class<?> clazz;
    	ClassResolver resolver;
    	private Resolver(Class<?> clazz) {
    		this.clazz=clazz;
    		resolver=TypeUtils.createClassResolver(clazz);
    	}
    	
    	ResolvedClass resolve(java.lang.reflect.Field f) {
    		return resolver.resolve(f);
    	}
    	
    	public String getName() {
    		return clazz.getName();
    	}
    }
}
