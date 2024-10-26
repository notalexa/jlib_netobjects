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
import java.lang.reflect.Method;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.utils.TypeUtils;
import not.alexa.netobjects.utils.TypeUtils.ResolvedClass;

/**
 * Represents a linked field in a network object.
 * 
 * @author notalexa
 *
 */
public final class FieldAccessor {
	private static final ResolvedClass OBJECT_CLASS=TypeUtils.createClassResolver(Object.class).resolve(Object.class);
	
    public static FieldAccessor createUnknown(String clazz,String field) {
    	return new FieldAccessor(clazz+"."+field);
    }

	private ResolvedClass type;
	private Getter getter;
	private Setter setter;
	
	private FieldAccessor(String name) {
		type=OBJECT_CLASS;
		getter=new Getter() {
			@Override
			public Object invoke(AccessContext context, Object o) throws Throwable {
				throw new BaseException(BaseException.NOT_FOUND,"Field "+name+" is unknown.");
			}
		};
		setter=new Setter() {
			@Override
			public void invoke(AccessContext context, Object o, Object v) throws Throwable {
				throw new BaseException(BaseException.NOT_FOUND,"Field "+name+" is unknown.");
			}
		};
	}
	private FieldAccessor(ResolvedClass type,Getter getter,Setter setter) {
		this.type=type;
		this.getter=getter;
		this.setter=setter;
	}
	
	public FieldAccessor(String name, ResolvedClass type,Field field,Method getter,Method setter) {
		this.type=type;
		if(getter!=null) {
			getter.setAccessible(true);
			switch(RuntimeInfoHelper.methodPrio(0, getter)) {
    			case 1: this.getter=new Getter() {
					@Override
					public Object invoke(AccessContext context, Object o) throws Throwable {
						return getter.invoke(o);
					}
    			};
    			break;
    			case 2: this.getter=new Getter() {
					@Override
					public Object invoke(AccessContext context, Object o) throws Throwable {
						return getter.invoke(o,context.getContext());
					}
    			};
    			break;
    			case 3: this.getter=new Getter() {
					@Override
					public Object invoke(AccessContext context, Object o) throws Throwable {
						return getter.invoke(o,context);
					}
    			};
    			break;
			}
		} else if(field!=null) {
			field.setAccessible(true);
			this.getter=new Getter() {
				@Override
				public Object invoke(AccessContext context, Object o) throws Throwable {
					return field.get(o);
				}
			};
		}
		if(this.getter==null) {
			this.getter=new Getter() {
				@Override
				public Object invoke(AccessContext context, Object o) throws Throwable {
					throw new BaseException(BaseException.FORBIDDEN,"Field "+name+" is write only");
				}
			};
		}
		if(setter!=null) {
			setter.setAccessible(true);
			switch(RuntimeInfoHelper.methodPrio(1, setter)) {
			case 1: this.setter=new Setter() {
					@Override
					public void invoke(AccessContext context, Object o,Object v) throws Throwable {
						setter.invoke(o,v);
						
					}
    			};
    			break;
			case 2: this.setter=new Setter() {
					@Override
					public void invoke(AccessContext context, Object o,Object v) throws Throwable {
						setter.invoke(o,context.getContext(),v);
					}
    			};
    			break;
			case 3: this.setter=new Setter() {
					@Override
					public void invoke(AccessContext context, Object o,Object v) throws Throwable {
						setter.invoke(o,context,v);
					}
    			};
    			break;
			}
		} else if(field!=null) {
			field.setAccessible(true);
			this.setter=new Setter() {
				@Override
				public void invoke(AccessContext context, Object o,Object v) throws Throwable {
					field.set(o,v);
				}
			};
		}
		if(this.setter==null) {
			this.setter=new Setter() {
				@Override
				public void invoke(AccessContext context, Object o,Object v) throws Throwable {
					throw new BaseException(BaseException.FORBIDDEN,"Field "+name+" is read only");
				}
			};
		}
	}

    public ResolvedClass getFieldType() {
    	return type;
    }
    
    public Object get(AccessContext context,Object o) throws BaseException {
		if(getter!=null) try {
			return getter.invoke(context, o);
		} catch(Throwable t) {
			return BaseException.throwException(t);
		} else {
			return null;
		}
    }
    public void set(AccessContext context,Object o,Object v) throws BaseException {
		if(setter!=null) try {
			setter.invoke(context, o, v);
		} catch(Throwable t) {
			BaseException.throwException(t);
		}
    }
    
    public FieldAccessor filter(CodingFilter filter) {
    	return new FieldAccessor(type,filter.filter(getter),filter.filter(setter));
    	
    }
    
    
    public interface Setter {
    	public void invoke(AccessContext context,Object o,Object v) throws Throwable;
    }
    public interface Getter {
    	public Object invoke(AccessContext context,Object o) throws Throwable;
    }    
}
