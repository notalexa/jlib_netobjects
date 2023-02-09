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

import java.util.Collection;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.Flavour;

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
            fields[i]=createFieldAccess(clazz,clazz,classFields[i]);
        }
    }
    
    protected NField createFieldAccess(Class<?> original,Class<?> clazz,Field f) {
        if(clazz!=null) {
            try {
                java.lang.reflect.Field classField=clazz.getDeclaredField(f.getName());
                classField.setAccessible(true);
                return new NField.FieldImpl(classField);
            } catch(Throwable t) {
                
            }
            return createFieldAccess(original,clazz.getSuperclass(),f);
        } else {
            return new NField.UnknownField(original.getName(),f.getName());
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
        if(f.getType().getFlavour()==Flavour.ArrayType) {
            Class<?> type=fields[f.getIndex()].getFieldType();
            if(type.isArray()) {
                return forArray(f.getType(), type);
            } else if(Collection.class.isAssignableFrom(type)) {
                return forCollection(f.getType(),type);
            } else if(Map.class.isAssignableFrom(type)) {
                return forMap(f.getType(),(Class<Map>)type);
            } else {
            	throw new BaseException(BaseException.GENERAL,"Unsupported field access: "+type+" is not an array type (most likely, this is a known generic type parameter bug).");
            }
        }
        return super.createFieldAccess(f);
    }
}
