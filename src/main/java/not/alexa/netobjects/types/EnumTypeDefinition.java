/*
 * Copyright (C) 2020 Not Alexa
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

import java.util.List;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;

/**
 * Enumeration types can have a finite set of values. Each value can be represented
 * as a string or an integer value.
 * <br>A hash over the set of values can be used to speed up a typical equals check.
 * 
 * @author notalexa
 *
 */
public class EnumTypeDefinition extends TypeDefinition {
    private static final Value[] NO_VALUES=new Value[0];
	/**
	 * 
	 * @return the type definition of an enum type definition
	 */
	public static ClassTypeDefinition getTypeDescription() {
		return Types.ENUM_TYPE;
	}
	private int h;
	private Value[] values=NO_VALUES;
	private EnumTypeDefinition() {}
	
	/**
	 * An anonymous enum type with the given values
	 * 
	 * @param values the values of this enum
	 */
	public EnumTypeDefinition(Value...values) {
		super();
		setValues(values);
	}
	
	/**
	 * An enumeration constructed from the given type.
	 * 
	 * @param type the (java) enum type used for constructing this enum type
	 */
	public EnumTypeDefinition(Class<? extends Enum<?>> type) {
		super(type);
		Enum<?>[] constants=type.getEnumConstants();
		Value[] values=new Value[constants.length];
		for(int i=0;i<values.length;i++) {
			values[i]=new Value(i,constants[i].toString());
		}
		setValues(values);
	}
	
	@Override
	public Flavour getFlavour() {
		return Flavour.EnumType;
	}
	
	private void setValues(Value[] values) {
		h=0;
		this.values=values;
		for(int i=0;i<values.length;i++) {
			h=(h<<1)^this.values[i].hashCode();
		}
		h^=values.length;
		
	}

	/**
	 * 
	 * @return the possible values of this enumeration
	 */
	public Value[] getValues() {
		return values;
	}
	
	public int hashCode() {
		return h;
	}
	
	public boolean equals(Object other) {
		if(other.hashCode()==h&&other instanceof EnumTypeDefinition) {
			EnumTypeDefinition e=(EnumTypeDefinition)other;
			if(check(e)==0&&e.values.length==values.length) {
				// Consistent and equals
				for(int i=0;i<values.length;i++) {
					if(!values[i].equals(e.values[i])) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Class representing one enum value. Each value has an index (<code>ordinal()</code> for a java enum) and a value (or code).
	 * <br>Two values represents the same constant, if the index and code equals.
	 * 
	 * @author notalexa
	 *
	 */
	public static class Value {
		private int index;
		private String value;
		Value() {}
		/**
		 * Construct a value
		 * 
		 * @param index the index of this value
		 * @param value the value (or code) of this value
		 */
		public Value(int index,String value) {
			this.index=index;
			this.value=value;
		}
		
		/**
		 * 
		 * @return the index of this enum constant
		 */
		public int getIndex() {
			return index;
		}
		
		/**
		 * 
		 * @return the value of this enum constant
		 */
		public String getEnumValue() {
			return value;
		}
		
		/**
		 * Same as {@link #getEnumValue()}
		 * 
		 * @return the code of this value
		 */
		public String getCode() {
			return value;
		}
		
		public int hashCode() {
			return index^value.hashCode();
		}
		
		public boolean equals(Object other) {
			if(other instanceof Value) {
				Value v=(Value)other;
				return v.index==index&&v.value.equals(value);
			}
			return false;
		}
		
		/**
		 * Class access for an enum value
		 * 
		 * @author notalexa
		 *
		 */
		public static class ClassAccess extends AbstractClassAccess implements Access {
			public ClassAccess(AccessFactory factory) {
				super(factory,Types.VALUE_TYPE);
			}
			
			@Override
			public AccessibleObject newInstance(AccessContext context) throws BaseException {
				return new DefaultAccessibleObject(this,new Value());
			}
			
			@Override
			public Object getField(Object o, int index) throws BaseException {
				Value def=(Value)o;
				switch(index) {
					case 0:return def.index;
					case 1:return def.value;
				}
				return null;
			}

			@Override
			public void setField(Object o, int index, Object v) throws BaseException {
				Value def=(Value)o;
				switch(index) {
					case 0:def.index=(Integer)v;
						break;
					case 1:def.value=(String)v;
						break;
				}
			}
		}
	}
	
	/**
	 * Class access for an enum type definition
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
			return new DefaultAccessibleObject(this,new EnumTypeDefinition());
		}
		
		@Override
		public Object getField(Object o, int index) throws BaseException {
			EnumTypeDefinition def=(EnumTypeDefinition)o;
			switch(index) {
				case 0:List<ObjectType> types=def.getTypes();
                    return types.size()==0?null:types.toArray(new ObjectType[types.size()]);
				case 1:return def.values.length==0?null:def.values;
			}
			return null;
		}

		@Override
		public void setField(Object o, int index, Object v) throws BaseException {
			EnumTypeDefinition def=(EnumTypeDefinition)o;
			switch(index) {
				case 0:def.addTypes((ObjectType[])v);
					break;
				case 1:def.setValues((Value[])v);
					break;
			}
		}

		@Override
		public Access createFieldAccess(Field f) throws BaseException {
			switch(f.getIndex()) {
				case 0:return forArray(f.getType(),ObjectType[].class);
				case 1:return forArray(f.getType(),Value[].class);
			}
			return null;
		}		
	}
}
