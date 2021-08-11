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
package not.alexa.netobjects.types;

import java.util.Arrays;
import java.util.List;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;

public class MethodTypeDefinition extends TypeDefinition {
	private static final TypeDefinition[] NO_TYPES=new TypeDefinition[0];
	
	public static ClassTypeDefinition getTypeDescription() {
		return Types.METHOD_TYPE;
	}
	int h;
	String name;
	TypeDefinition classType;
	TypeDefinition[] parameterTypes;
	TypeDefinition[] returnTypes;
	public MethodTypeDefinition() {}
	
	public MethodTypeDefinition(TypeDefinition classType,String name) {
		this(classType,name,NO_TYPES,NO_TYPES);
	}
	
	/**
	 * We allow mutable anonymous type definitions while constructing this definition.
	 * 
	 * @param name the name of this method
	 * @param classType the type of the underlying (defining) object type
	 * @param parameterTypes the list of parameter types
	 * @param returnTypes the list of return types
	 */
	public MethodTypeDefinition(TypeDefinition classType,String name,TypeDefinition[] parameterTypes,TypeDefinition[] returnTypes) {
		super();
		this.name=name;
		this.classType=classType;
		this.parameterTypes=parameterTypes;
		this.returnTypes=returnTypes;
		h=name.hashCode()^typeHash(parameterTypes)^typeHash(returnTypes);
	}
	
	@Override
	public Flavour getFlavour() {
		return Flavour.MethodType;
	}
	
	public String getName() {
		return name;
	}
	
	MethodTypeDefinition forClass(TypeDefinition classType) {
		return new MethodTypeDefinition(classType,name,parameterTypes,returnTypes);
	}
	
	/**
	 * 
	 * @return the (optional) type of the underlying defining class. If <code>null</code>, this method is not related to a class.
	 */
	public TypeDefinition getClassType() {
		return classType;
	}
	
	public TypeDefinition[] getParameterTypes() {
		return parameterTypes;
	}
	
	public TypeDefinition[] getReturnTypes() {
		return returnTypes;
	}
	
	/**
	 * A builder to modify this definition. The builder inherits name (which cannot be modified) and parameter and return types (which can
	 * be modified).
	 * 
	 * @return a builder to modify parameter and return types
	 */
	public Builder createBuilder() {
		return new Builder();
	}
	
	public int hashCode() {
		return h;
	}
	
	public boolean equals(Object o) {
		if(o instanceof MethodTypeDefinition&&o.hashCode()==h) {
			if(o==this) {
				return true;
			}
			MethodTypeDefinition def=(MethodTypeDefinition)o;
			if(def.name.equals(name)&&def.parameterTypes.length==parameterTypes.length&&def.returnTypes.length==returnTypes.length) {
				if(!Arrays.equals(def.parameterTypes,parameterTypes)) {
					return false;
				}
				if(!Arrays.equals(def.returnTypes,returnTypes)) {
					return false;
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Builder class for a method type definition.
	 * 
	 * @author notalexa
	 *
	 */
	public class Builder {
		private TypeDefinition[] parameterTypes=MethodTypeDefinition.this.parameterTypes;
		private TypeDefinition[] returnTypes=MethodTypeDefinition.this.returnTypes;
		
		/**
		 * 
		 * @return a method type definition build from the builders material
		 */
		public MethodTypeDefinition build() {
			return new MethodTypeDefinition(classType,name,parameterTypes,returnTypes);
		}
		
		/**
		 * Modify the parameter types of this method.
		 * 
		 * @param parameterTypes the new parameter types
		 * @return <code>this</code>
		 */
		public Builder setParameterTypes(TypeDefinition...parameterTypes) {
			this.parameterTypes=parameterTypes;
			return this;
		}

		/**
		 * Modify the return types of this method.
		 * 
		 * @param returnTypes the new return types
		 * @return <code>this</code>
		 */
		public Builder setReturnTypes(TypeDefinition...returnTypes) {
			this.returnTypes=returnTypes;
			return this;
		}		
	}
	
	/**
	 * Class access for a method type definition
	 * 
	 * @author notalexa
	 *
	 */
	public static class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory) {
			super(factory,getTypeDescription());
		}
		
		@Override
		public AccessibleObject newInstance(AccessContext context) throws BaseException {
			return new DefaultAccessibleObject(this,new MethodTypeDefinition());
		}
		
		@Override
		public Object getField(Object o, int index) throws BaseException {
			MethodTypeDefinition def=(MethodTypeDefinition)o;
			switch(index) {
				case 0:List<ObjectType> types=def.getTypes();
                    return false&&types.size()==0?null:types.toArray(new ObjectType[types.size()]);
				case 1:return def.name;
				case 2:return def.parameterTypes.length==0?null:def.parameterTypes;
				case 3:return def.returnTypes.length==0?null:def.returnTypes;
			}
			return null;
		}

		@Override
		public void setField(Object o, int index, Object v) throws BaseException {
			MethodTypeDefinition def=(MethodTypeDefinition)o;
			switch(index) {
				case 0:def.addTypes((ObjectType[])v);
					break;
				case 1:def.name=(String)v;//setValues((Value[])v);
					break;
				case 2:def.parameterTypes=(TypeDefinition[])v;//setValues((Value[])v);
					break;
				case 3:def.returnTypes=(TypeDefinition[])v;//setValues((Value[])v);
					break;
			}
		}

		@Override
		public Access createFieldAccess(Field f) throws BaseException {
			switch(f.getIndex()) {
				case 0:return forArray(f.getType(),ObjectType[].class);
				case 2:return forArray(f.getType(),TypeDefinition[].class);
				case 3:return forArray(f.getType(),TypeDefinition[].class);
			}
			return super.createFieldAccess(f);
		}
		
		public Object finish(Object o) {
			MethodTypeDefinition def=(MethodTypeDefinition)o;
			if(def.parameterTypes==null) {
				def.parameterTypes=NO_TYPES;
			}
			if(def.returnTypes==null) {
				def.returnTypes=NO_TYPES;
			}
			def.h=def.name.hashCode()^typeHash(def.parameterTypes)^typeHash(def.returnTypes);
			return o;
		}
	}

}
