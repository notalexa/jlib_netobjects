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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.access.ReflectionClassAccess.Resolver;
import not.alexa.netobjects.types.access.RuntimeInfo.DefaultProvider;
import not.alexa.netobjects.types.access.RuntimeInfo.DeferredProvider;
import not.alexa.netobjects.types.access.RuntimeInfo.FieldMapper;
import not.alexa.netobjects.types.access.RuntimeInfo.InjectorInfos;
import not.alexa.netobjects.types.access.RuntimeInfo.Provider;
import not.alexa.netobjects.utils.TypeUtils.ResolvedClass;

/**
 * Class containing information about the (reflexive) access to a concrete class.
 * <br>Typically, a resolver creates a type definition (using reflexion of a given class) and a
 * {@link Provider} using information contained in this class to create the access (for the class
 * and it's overlays).
 * <br>Using classes (and other reflected objects) would result in a cycle preventing the class itself from being
 * garbage collected.
 * 
 * @author notalexa
 */
public class ClassAccessInfo {
	private final String className;
	private final int no;
	private final InjectorInfos injectorInfos;
	private final FieldAccessInfo[] fieldAccess;
	
	public ClassAccessInfo(Class<?> clazz,int no,InjectorInfos infos) {
		this(clazz.getName(),no,infos,null);
	}
	
	private ClassAccessInfo(String classname,int no,InjectorInfos injectorInfos,FieldAccessInfo[] fieldAccess) {
		this.className=classname;
		this.no=no;
		this.injectorInfos=injectorInfos!=null?injectorInfos.get():null;
		this.fieldAccess=fieldAccess;
	}
	
	public ClassAccessInfo forFieldAccess(FieldAccessInfo[] fieldAccess) {
		return new ClassAccessInfo(className, no,injectorInfos,fieldAccess);
	}

	public boolean hasConstructorInfos() {
		return no>=0;
	}
	
	/**
	 * Return the constructor referenced in this object for a concrete class.
	 * <br>The implementation needs to be careful about enclosing classes if the root class itself is not an
	 * inner class but the overlay is.
	 * 
	 * @param enclosingClass the enclosing class of the class
	 * @param clazz the class itself (either the root class or an overlay)
	 * @return a constructor for the class.
	 */
	public java.lang.reflect.Constructor<?> get(Class<?> enclosingClass, Class<?> clazz) {
		if(no<0) {
			// No constructor defined. Throw an exception later
			return null;
		}
		if(clazz.getName().equals(className)) {
			java.lang.reflect.Constructor<?> c=clazz.getDeclaredConstructors()[no];
			c.setAccessible(true);
			return c;
		} else try {
			Class<?>[] parameterTypes=getParameterTypes(enclosingClass!=null,clazz.getSuperclass());
			if(enclosingClass!=null) {
				parameterTypes[0]=enclosingClass;
			}
			java.lang.reflect.Constructor<?> c=clazz.getDeclaredConstructor(parameterTypes);
			c.setAccessible(true);
			return c;
		} catch(Throwable t) {
			return null;
		}
	}
	
	private Class<?>[] getParameterTypes(boolean inner,Class<?> clazz) {
		if(clazz==null) {
			return null;
		} else if(clazz.getName().equals(className)) {
			Class<?>[] parameterTypes=clazz.getDeclaredConstructors()[no].getParameterTypes();
			if(inner&&RuntimeInfo.getEnclosingClass(clazz)==null) {
				parameterTypes=Arrays.copyOf(parameterTypes, parameterTypes.length-1);
				for(int i=parameterTypes.length-1;i>1;i--) {
					parameterTypes[i]=parameterTypes[i-1];
				}
			}
			return parameterTypes;
		} else {
			return getParameterTypes(inner,clazz.getSuperclass());
		}
	}
	
	public FieldAccessInfo getField(int n) {
		return fieldAccess[n];
	}
	
    public static class AccessRef {
    	String className;
    	int no=-1;
    	int prio;
    	public AccessRef(Field f) {
    		className=f.getDeclaringClass().getName();
    		for(int i=0;i<f.getDeclaringClass().getDeclaredFields().length;i++) {
    			if(f.getDeclaringClass().getDeclaredFields()[i].equals(f)) {
    				no=i;
    				break;
    			}
    		}
    		if(no<0) {
    			throw new RuntimeException();
    		} else {
    			no=-no;
    		}
    		prio=0;
    	}
    	public AccessRef(Method m) {
    		className=m.getDeclaringClass().getName();
    		for(int i=0;i<m.getDeclaringClass().getDeclaredMethods().length;i++) {
    			if(m.getDeclaringClass().getDeclaredMethods()[i].equals(m)) {
    				no=i;
    				break;
    			}
    		}
    		if(no<0) {
    			throw new RuntimeException();
    		} else {
    			no++;
    		}
    		if(m.getReturnType().equals(Void.TYPE)) {
    			if(m.getParameterCount()==2) {
    				Class<?> clazz=m.getParameterTypes()[0];
    				if(AccessContext.class.isAssignableFrom(clazz)) {
    					prio=3;
    				} else if(Context.class.isAssignableFrom(clazz)) {
    					prio=2;
    				}
    			} else {
    				prio=1;
    			}
    		} else {
    			if(m.getParameterCount()==1) {
    				Class<?> clazz=m.getParameterTypes()[0];
    				if(AccessContext.class.isAssignableFrom(clazz)) {
    					prio=3;
    				} else if(Context.class.isAssignableFrom(clazz)) {
    					prio=2;
    				}
    			} else {
    				prio=1;
    			}
    		}
    	}
    	
    	public boolean isField() {
    		return no<=0;
    	}
    	
    	public Field getField(Class<?> clazz) {
    		if(no<=0&&clazz!=null) {
    			if(clazz.getName().equals(className)) {
    				return clazz.getDeclaredFields()[-no];
    			} else {
    				return getField(clazz.getSuperclass());
    			}
    		} else {
    			return null;
    		}
    	}
    	
    	public Method getMethod(Class<?> clazz) {
    		if(no>0&&clazz!=null) {
    			if(clazz.getName().equals(className)) {
    				return clazz.getDeclaredMethods()[no-1];
    			} else {
    				return getMethod(clazz.getSuperclass());
    			}
    		} else {
    			return null;
    		}
    	}
    }
    
    public static class FieldAccessInfo {
    	AccessRef getter;
    	AccessRef setter;
    	public FieldAccessInfo(AccessRef getter,AccessRef setter) {
    		this.getter=getter;
    		this.setter=setter;
    	}
    	
    	public boolean isReadOnly() {
    		return setter==null&&(getter==null||!getter.isField());
    	}
    	
    	public FieldAccessor create(Resolver resolver, Class<?> clazz,not.alexa.netobjects.types.ClassTypeDefinition.Field f) {
    		Field getterField=getter==null?null:getter.getField(clazz);
    		Field setterField=setter==null?null:setter.getField(clazz);
    		Method getterMethod=getter==null?null:getter.getMethod(clazz);
    		Method setterMethod=setter==null?null:setter.getMethod(clazz);
    		if(getterField==null&&getterMethod==null) {
    			return null;
    		} else {
    			if(getterField!=null) {
    				if(setterField!=null&&!setterField.equals(getterField)) {
    					throw new RuntimeException("Field mismatch");
    				}
    			} else {
    				getterField=setterField;
    			}
    			ResolvedClass fieldType=getterMethod!=null?resolver.resolve(getterMethod):resolver.resolve(getterField);
    			FieldAccessor field=new FieldAccessor(clazz.getName()+"."+f.getName(),fieldType,getterField,getterMethod,setterMethod);
    			if(f.getType().getFlavour()==Flavour.PrimitiveType) {
    				Class<?> primitiveClassType=f.getType().asClass(clazz.getClassLoader());
    				if(!primitiveClassType.isAssignableFrom(fieldType.getCodingClass())) {
    					CodingFilter<?,?> filter=resolver.getFilter(fieldType.getCodingClass());
    					if(filter!=null) {
    						field=filter.filter(primitiveClassType,field);
    					}
    				}
    			}
    			return field;
    		}
		}

		public FieldAccessInfo merge(FieldAccessInfo former) {
			if(former!=null) {
	    		if(former.getter!=null&&(getter==null||former.getter.prio>getter.prio)) {
	    			getter=former.getter;
	    		}
	    		if(former.setter!=null&&(setter==null||former.setter.prio>setter.prio)) {
	    			setter=former.setter;
	    		}
			}
    		return this;
    	}
    }
    
    private void check(Class<?> clazz) {
    	while(clazz!=null) {
    		if(clazz.getName().equals(className)) {
    			return;
    		}
    		clazz=clazz.getSuperclass();
    	}
    	throw new RuntimeException("Illegal provider call. class mismatch");
    }

    /**
     * Create a provider for this access infos.
     * 
     * @param clazz the class we need a provider for
     * @return the provider for the given class.
     */
    public Provider getProvider(Class<?> clazz) {
    	return getProvider(clazz,injectorInfos,FieldMapper.IDENTITY);
    	
    }
    
    /**
     * Create a provider for this access infos with a concrete field mapping.
     * 
     * @param clazz the class we need a provider for
     * @param fieldMap the field map
     * @return the provider for the given class
     */
    public Provider getProvider(Class<?> clazz, InjectorInfos injectorInfos,FieldMapper fieldMap) {
    	check(clazz);
    	return new DefaultProvider(clazz, injectorInfos,fieldMap) {
			@Override
			protected FieldAccessor createFieldAccess(Resolver resolver, Class<?> clazz, ClassTypeDefinition.Field f) {
				ClassAccessInfo.FieldAccessInfo info=getField(f.getIndex());
				return info==null?FieldAccessor.createUnknown(clazz.getName(), f.getName()):info.create(resolver, clazz,f);
			}    		
    	};
    }

    /**
     * Create a provider for this access infos.
     *  
     * @param clazz the class we need a provider for
     * @param constructorFields the list of fields to be used in the constructor (can be empty)
     * @param fieldMap the field map
     * @return the provider for the given class
     */
    public Provider getProvider(Class<?> clazz, List<String> constructorFields, InjectorInfos injectorInfos, FieldMapper fieldMap) {
    	check(clazz);
    	if(this.injectorInfos!=null) {
    		injectorInfos=this.injectorInfos.add(injectorInfos);
    	}
    	if(constructorFields.size()==0&&(injectorInfos==null||!injectorInfos.hasParameterFields())) {
    		return getProvider(clazz, injectorInfos, fieldMap);
    	} else {
    		return new ClassAccessProvider(clazz, constructorFields, injectorInfos,fieldMap);
    	}
    }
    
    class ClassAccessProvider extends DeferredProvider {
    	public ClassAccessProvider(Class<?> clazz, List<String> constructorFields, InjectorInfos injectorInfos, FieldMapper fieldMap) {
			super(clazz, constructorFields, injectorInfos, fieldMap);
		}

		@Override
		public java.lang.reflect.Constructor<?> findConstructor(Class<?> enclosingClass, Class<?> clazz) throws Throwable {
			return ClassAccessInfo.this.get(enclosingClass,clazz);
		}

		@Override
		public FieldAccessor createFieldAccess(ReflectionClassAccess.Resolver resolver, Class<?> clazz, ClassTypeDefinition.Field f) {
			ClassAccessInfo.FieldAccessInfo info=getField(f.getIndex());
			if(info==null) {
				String fieldName=resolver.mapField(clazz, f.getName());
				try {
					Class<?> enclosingClass=RuntimeInfo.getEnclosingClass(clazz);
					java.lang.reflect.Constructor<?> c=findConstructor(RuntimeInfo.getEnclosingClass(clazz), clazz);
					for(int i=0;i<constructorFields.size();i++) {
						if(fieldName.equals(constructorFields.get(i))) {
							ResolvedClass fieldClass=resolver.resolve(c.getParameters()[i+(enclosingClass==null?0:1)]);
							return new FieldAccessor(clazz.getName()+"."+f.getName(),fieldClass,null,null,null);
						}
					}
				} catch(Throwable t) {
				}
				return FieldAccessor.createUnknown(clazz.getName(), fieldName);
			} else {
				return info.create(resolver,clazz,f);
			}
		}
	}
}
