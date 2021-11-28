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

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.Lambda;
import not.alexa.netobjects.types.MethodTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.utils.Cursor;
import not.alexa.netobjects.utils.Sequence;

/**
 * As mentioned in the introduction of {@link Access}, an accessible object can be constructed out of access and a general object by "contraction".
 * This approach is provided by this class.
 * @author notalexa
 *
 */
public class DefaultAccessibleObject implements AccessibleObject,Sequence<AccessibleObject> {
	protected Access access;
	protected Object o;
	
	/**
	 * Extensions of this class may serve as the object itself. In this case, only access is needed.
	 * 
	 * @param access the access for this object
	 */
	public DefaultAccessibleObject(Access access) {
		this.access=access;
		o=this;
	}
	
	/**
	 * In the general case, the object itself is exterior to this class and needs to be specified.
	 * 
	 * @param access the access for the object
	 * @param o the object itself
	 */
	public DefaultAccessibleObject(Access access,Object o) {
		this.access=access;
		this.o=o;
	}
	
	@Override
	public void setField(Field f,AccessibleObject v) throws BaseException {
		access.setField(o,f,access.getObject(v));
	}

	@Override
	public TypeDefinition getType() {
		return access.getType();
	}

	@Override
	public Object getObject() {
		return o;
	}

	@Override
	public AccessibleObject getField(Field f) throws BaseException {
		return access.makeAccessible(access.getField(o,f));
	}
	
	@Override
	public Object getAssignable() throws BaseException {
		return access.finish(getObject());
	}

	@Override
	public boolean busy() {
		return true;
	}

	@Override
	public AccessibleObject current() {
		return this;
	}

	@Override
	public Sequence<AccessibleObject> next() {
		return Sequence.emptySequence();
	}

	@Override
	public Object getPrimitiveOrEnumField(Field f) throws BaseException {
		return access.getField(o, f);
	}

	@Override
	public Sequence<AccessibleObject> asSequence() {
		return this;
	}

    @Override
    public AccessibleObject getMethod(MethodTypeDefinition method) throws BaseException {
        return new AccessibleMethod(method);
    }
    
    class AccessibleMethod implements AccessibleObject,Sequence<AccessibleObject> {
        MethodTypeDefinition methodType;
        AccessibleMethod(MethodTypeDefinition methodType) {
            this.methodType=methodType;
        }
        
        @Override
        public TypeDefinition getType() {
            return methodType;
        }

        @Override
        public Object getObject() {
            return null;
        }

        @Override
        public Sequence<AccessibleObject> asSequence() {
            return this;
        }

        @Override
        public AccessibleObject[] call(Context context, AccessibleObject... params) throws BaseException {
            Type methodId=methodType.getJavaClassType();
            if(methodId!=null) {
                TypeDefinition[] returnTypes=methodType.getReturnTypes();
                if(returnTypes.length<=1) {
                    Object[] args=new Object[params.length];
                    for(int i=0;i<params.length;i++) {
                        args[i]=params[i].getObject();
                    }
                    Lambda l=new Lambda(getObject(),methodId,args);
                    Object result=l.call(context);
                    if(returnTypes.length==0) {
                        return new AccessibleObject[0];
                    }
                    Access resultAccess=access.getFactory().resolve(context,returnTypes[0]);
                    return new AccessibleObject[] { resultAccess.makeAccessible(result) };
                } else {
                    throw new BaseException(BaseException.BAD_REQUEST,"Unsupported: Methods with return more than one return values");
                }
            }
            throw new BaseException(BaseException.BAD_REQUEST,"Method has no id");
        }

        @Override
        public boolean busy() {
            return true;
        }

        @Override
        public AccessibleObject current() {
            return this;
        }

        @Override
        public Cursor<AccessibleObject> next() {
            return Sequence.emptySequence();
        }
    }
}
