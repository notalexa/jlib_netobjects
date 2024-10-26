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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.utils.TypeUtils;
import not.alexa.netobjects.utils.TypeUtils.ClassResolver;
import not.alexa.netobjects.utils.TypeUtils.ResolvedClass;

/**
 * Standard class access using reflection ({@link FieldAccessor}. The field is resolved as follows:
 * Beginning at the class, the field is resolved using the {@code getDeclaredField} method on
 * {@code java.lang.reflect.Field}.
 * 
 * @author notalexa
 *
 */
public class ReflectionClassAccess extends AbstractClassAccess {
	private ClassLoader classLoader;
	private Class<?> clazz;
    private FieldAccessor[] fields;
    ReflectionClassAccess(AccessFactory factory, ClassLoader classLoader,ResolvedClass clazz,ClassTypeDefinition classType, RuntimeInfo constructor) {
        super(factory, classType, constructor);
        this.classLoader=classLoader;
        this.clazz=clazz.getCodingClass();
        Field[] classFields=classType.getFields();
        fields=new FieldAccessor[classFields.length];
        for(int i=0;i<fields.length;i++) {
            fields[i]=createFieldAccess(new Resolver(clazz),clazz.getCodingClass(),classFields[i]);
        }
    }
    

	@Override
	public ClassLoader getAccessLoader() {
		return classLoader;
	}
    
    protected FieldAccessor createFieldAccess(Resolver resolver,Class<?> clazz,Field f) {
       	FieldAccessor field=constructor.createFieldAccess(resolver, clazz, f);
        return field==null?FieldAccessor.createUnknown(resolver.getName(),f.getName()):field;
    }
    
    @Override
    public Object getObject(AccessContext context,AccessibleObject o) throws BaseException {
    	Object result=super.getObject(context, o);
    	if(!clazz.isInstance(result)) {
    		result=context.getContext().cast(clazz, o);
    	}
    	return result;
    }
    
    @Override
    protected Object getField(AccessContext context,Object o, int index) throws BaseException {
        return fields[index].get(context,o);
    }

    @Override
    protected void setField(AccessContext context,Object o, int index, Object v) throws BaseException {
        fields[index].set(context,o, v);
    }
    
    @Override
    public Access createFieldAccess(Field f) throws BaseException {
    	ResolvedClass fieldClass=fields[f.getIndex()].getFieldType();
    	Access access=createAccess(f.getType(),fields[f.getIndex()].getFieldType());
    	if(!fieldClass.getResolvedClass().equals(fieldClass.getCodingClass()) ) {
    		return makeCodingAccess(access);
    	}
    	return access;
    }
    
    private static Access makeCodingAccess(Access access) {
		return new DelegatingAccess(access) {
			public Object getObject(AccessContext context, AccessibleObject o) throws BaseException {
				return o.getAssignable(context);
			}
		};
    }
    
    public class Resolver {
    	Class<?> clazz;
    	ClassResolver resolver;
    	private Resolver(ResolvedClass resolvedClass) {
    		this.clazz=resolvedClass.getResolvedClass();
    		resolver=resolvedClass==null?TypeUtils.createClassResolver(clazz):resolvedClass.asResolver();
    	}
    	
    	public Class<?> getResolverClass() {
    		return clazz;
    	}
    	
    	public String mapField(Class<?> clazz, String name) {
    		return constructor.mapField(clazz, name);
    	}
    	
    	public ResolvedClass resolve(java.lang.reflect.Field f) {
    		return resolver.resolve(f);
    	}
    	
    	public ResolvedClass resolve(Method m) {
    		return resolver.resolve(m);
    	}

    	public ResolvedClass resolve(Parameter p) {
    		return resolver.resolve(p);
    	}

    	public ResolvedClass resolve(Class<?> clazz) {
    		return resolver.resolve(clazz);
    	}
    	
    	public String getName() {
    		return clazz.getName();
    	}

		public CodingFilter getFilter(Class<?> codingClass) {
			return RuntimeInfoHelper.getFilter(codingClass);
		}
    }
}
