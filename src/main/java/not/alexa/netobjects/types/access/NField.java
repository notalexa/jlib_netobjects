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

import java.lang.reflect.Field;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.utils.TypeUtils;
import not.alexa.netobjects.utils.TypeUtils.ResolvedClass;

/**
 * Represents a linked field in a network object.
 * 
 * @author notalexa
 *
 */
public interface NField {
    public ResolvedClass getFieldType();
    public Object get(Object o) throws BaseException;
    public void set(Object o,Object v) throws BaseException;
    
    /**
     * Default field implementation accessing the field directly.
     * 
     * @author notalexa
     *
     */
    public class FieldImpl implements NField {
    	private ResolvedClass type;
        private Field f;
        FieldImpl(ResolvedClass type,Field f) {
        	this.type=type;
            this.f=f;
        }

        @Override
        public Object get(Object o) throws BaseException {
            try {
                return f.get(o);
            } catch(Throwable t) {
                return BaseException.throwException(t);
            }
        }

        @Override
        public void set(Object o, Object v) throws BaseException {
            try {
                f.set(o,v);
            } catch(Throwable t) {
                BaseException.throwException(t);
            }
        }

        @Override
        public ResolvedClass getFieldType() {
            return type;
        }        
    }
    
    /**
     * Field implementation throwing an error indicating that the field is not present.
     * 
     * @author notalexa
     *
     */
    public class UnknownField implements NField {
    	private static final ResolvedClass OBJECT_CLASS=TypeUtils.createClassResolver(Object.class).resolve(Object.class);
        private BaseException e;
        
        public UnknownField(String clazz,String field) {
            e=new BaseException(BaseException.NOT_FOUND,"Field "+field+" is unknown in "+clazz);
        }
        
        @Override
        public ResolvedClass getFieldType() {
            return OBJECT_CLASS;
        }

        @Override
        public Object get(Object o) throws BaseException {
            throw e;
        }

        @Override
        public void set(Object o, Object v) throws BaseException {
            throw e;
        }        
    }
}
