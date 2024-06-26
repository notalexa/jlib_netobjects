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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.api.Final;
import not.alexa.netobjects.types.EnumTypeDefinition.Value;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.ArrayTypeAccess;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;
import not.alexa.netobjects.types.access.EmptyArray;
import not.alexa.netobjects.types.access.EnumConstant;

@Final
public class ClassTypeDefinition extends AbstractClassTypeDefinition {
    private static final Field[] NO_FIELDS=new Field[0];
    private static final MethodTypeDefinition[] NO_METHODS=new MethodTypeDefinition[0];
	public static ClassTypeDefinition getTypeDescription() {
		return Types.CLASS_TYPE;
	}
	protected boolean enableObjectRefs;
	protected boolean extendible;
	protected Field[] fields;
	
	public ClassTypeDefinition(Class<?> clazz) {
		super(clazz);
	}
	
	public ClassTypeDefinition(ObjectType... types) {
		super(types);
	}

	@Override
	public Flavour getFlavour() {
		return Flavour.ClassType;
	}
	
	@Override
    public boolean isAbstract() {
        return extendible;
    }
    
	/**
	 * Add interfaces to this class type declaration. This interfaces are <b>not part of the global class definition</b> but can be 
	 * defined in a <b>local execution environment</b> and are therefore non persistent.
	 * 
	 * @param interfaces the interfaces to add
	 * @return this type description
	 */
	public ClassTypeDefinition addInterface(InterfaceTypeDefinition...interfaces) {
		super.addInterface(interfaces);
		return this;
	}
	
	/**
	 * If true, the codec should maintain a reference to an object of this
	 * type and serialize the reference after the first serialization.
	 * <br>Experience shows that this is not necessary in general even if
	 * an object is referenced twice in the original layout.
	 * 
	 * @return <code>true</code> if the type requires back references in the object stack
	 */
	public boolean enableObjectRefs() {
		return enableObjectRefs;
	}
	
	public Field[] getFields() {
		return fields;
	}
	
	public Builder createBuilder() {
		return new Builder();
	}
	
	@Override
	protected void calculateHash() {
		h=(enableObjectRefs?31:0)^Arrays.hashCode(methods);
		for(Field f:fields) {
			h^=f.hashCode();
		}
	}
	
	@Override
	protected boolean deepEquals(AbstractClassTypeDefinition other) {
		boolean ret=super.deepEquals(other);
		ClassTypeDefinition type=(ClassTypeDefinition)other;
		ret&=type.enableObjectRefs==enableObjectRefs;
		ret&=type.fields.length==fields.length;
		if(ret) for(int i=0;i<fields.length;i++) {
			ret&=type.fields[i].equals(fields[i]);
			if(!ret) {
				break;
			}
		}
		return ret;
	}

	public class Builder extends AbstractClassTypeDefinition.Builder<Builder> {
		protected boolean enableObjectRefs;
        protected boolean extendible;
		protected List<Field> fields=new ArrayList<>();
		public Builder setEnableObjectRefs(boolean enable) {
			enableObjectRefs=enable;
			return this;
		}
		
		protected Builder self() {
			return this;
		}
		
		/**
		 * Declare this class as abstract (that is fields
		 * of this type needs additional info because the class
		 * can be extended (like interfaces)).
		 * @param extendible {@code true} if the class should be considered as abstract
         * @return this builder for additional configuration
		 */
		public Builder setAbstract(boolean extendible) {
		    this.extendible=extendible;
		    return this;
		}
		
		
		/**
		 * Add a field with default configuration
		 * @param name the name of the field
		 * @param type the type of the field
		 * @return this builder for additional configuration
		 */
		public Builder addField(String name,TypeDefinition type) {
			return createField(name, type).build();
		}
		
		/**
		 * An abstract type can be added to a field if it is either not anonymous or immutable
		 * 
		 * @param name the name of the filed
		 * @param type it's type definition
		 * @return a builder for additional (field) setup
		 */
		public FieldBuilder createField(String name,TypeDefinition type) {
			if(type.isAnonymous()&&!type.isImmutable()) {
				new BaseException(BaseException.BAD_REQUEST,"Type of field "+ClassTypeDefinition.this+"."+name+" is anonymous and mutable.").throwRuntimeException();
			}
			return new FieldBuilder(name,type);
		}
		
		public ClassTypeDefinition build() {
			synchronized (ClassTypeDefinition.this) {
				if(!isImmutable()) {
					super.build();
					ClassTypeDefinition.this.fields=fields.toArray(new Field[fields.size()]);
					ClassTypeDefinition.this.enableObjectRefs=enableObjectRefs;
					ClassTypeDefinition.this.extendible=extendible;
					calculateHash();
				} else {
					new BaseException(BaseException.BAD_REQUEST,ClassTypeDefinition.this+" is immutable.").throwRuntimeException();
				}
				return ClassTypeDefinition.this;
			}
		}
		
		public class FieldBuilder {
			protected String name;
			protected boolean extendible;
            protected boolean optional;
            protected Object defaultValue;
			protected TypeDefinition type;
			protected Map<String,String> tags;
			protected Set<String> hints;
			
			protected int fieldIndex=-1;
			
			FieldBuilder(String name,TypeDefinition type) {
				this.name=name;
				this.type=type;
			}
			
			public FieldBuilder setIndex(int index) {
				if(index>=0) {
					fieldIndex=index;
				}
				return this;
			}
			
			public FieldBuilder addTag(String scheme,String tag) {
				if(tags==null) {
					tags=new HashMap<String, String>();
				}
				tags.put(scheme.toLowerCase(), tag);
				return this;
			}
			
			public FieldBuilder addHint(String hint) {
				if(hints==null) {
					hints=new HashSet<String>();
				}
				hints.add(hint);
				return this;
			}
			
			/**
			 * Should this field be considered as abstract?
			 * 
			 * @param extendible {@code true} if the <b>field</b> should be considered as abstract
			 * @return this builder for additional configuration
			 */
			public FieldBuilder setAbstract(boolean extendible) {
			    this.extendible=extendible;
			    return this;
			}
			
            public FieldBuilder setOptional(boolean optional) {
                this.optional=optional;
                return this;
            }
            
            public FieldBuilder setDefaultValue(Object defaultValue) {
                this.defaultValue=defaultValue;
                return this;
            }
            
			public Builder build() {
				Field f=new Field(fieldIndex<0?fields.size():fieldIndex,name,type);
                f.optional=optional;
                switch(type.getFlavour()) {
                    case InterfaceType:
                    case ClassType:f.extendible=extendible;
                        break;
                    case ArrayType:if("empty".equals(defaultValue)) {
	                    	f.defaultValue=new EmptyArray();
	                    }
	                    break;
                    case EnumType:if(defaultValue!=null) {
	                    	String val=defaultValue.toString();
	                    	for(Value v:((EnumTypeDefinition)type).getValues()) {
	                    		if(v.getEnumValue().equals(val)) {
	                    			f.defaultValue=new EnumConstant(val);
	                    		}
	                    	}
	                    }
                    	break;
                    case PrimitiveType:
                        // Check consistency
                        if(defaultValue!=null&&!ObjectType.createClassType(defaultValue.getClass()).equals(type.getJavaClassType())) {
                            throw new RuntimeException("Inconsistent default value: type is "+ObjectType.createClassType(defaultValue.getClass())+" but expected was "+type.getJavaClassType());
                        }
                        f.defaultValue=defaultValue;
                        break;
                }
				if(tags!=null) {
					f.tags=tags;
				}
				if(hints!=null) {
					f.hints=hints;
				}
				Builder.this.fields.add(f);
				return Builder.this;
			}
		}
	}
	
	@Final
	public class Field {
		private int h;
		protected int index;
		protected String name;
        protected boolean extendible;
        protected boolean optional;
		protected TypeDefinition type;
		protected Map<String,String> tags;
		protected Set<String> hints;
		protected Object defaultValue;
		
		private Field() {}
		protected Field(int index,String name,TypeDefinition type) {
			this.index=index;
			this.type=type;
			this.name=name;
			calculateHash();
		}
		
		private void calculateHash() {
			h=(index*31)^name.hashCode()^TypeDefinition.typeHash(type);
		}
		
		public int getIndex(String...schemata) {
			return index;
		}
		
		public boolean isDefault(Object o) {
			if(defaultValue!=null) {
				switch(type.getFlavour()) {
					case PrimitiveType:return o.equals(defaultValue);
					case EnumType:if(defaultValue instanceof EnumConstant) {
							return ((EnumConstant)defaultValue).represents(o);
						} else {
							return false;
						}
					case ArrayType:if(defaultValue instanceof EmptyArray) {
							return ((EmptyArray)defaultValue).represents(o);
						} else {
							return false;
						}
					default:
						break;
				}
			}
			return false;
		}
		
		/**
		 * 
		 * @return {@code true} if this field is abstract (that is either it's type or explicitly declared)
		 */
		public boolean isAbstract() {
		    return extendible||type.isAbstract();
		}
		
		public boolean isOptional() {
		    return optional;
		}
		
		public Object getDefaultValue() {
		    return defaultValue;
		}
		
		public String getName() {
			return name;
		}
		
		public TypeDefinition getType() {
			return type;
		}
		
		public String getTag(String...schemata) {
			if(tags!=null) for(String schema:schemata) {
				String tag=tags.get(schema.toLowerCase());
				if(tag!=null) {
					return tag;
				}
			}
			return name;
		}
		
		public boolean hasHint(String hint) {
			return hints!=null&&hints.contains(hint);
		}
		
		public ClassTypeDefinition getClassDescription() {
			return ClassTypeDefinition.this;
		}
		
		public int hashCode() {
			return h;
		}
		
		public boolean equals(Object o) {
			if(o==this) {
				return true;
			} else if(o instanceof Field) {
				Field f=(Field)o;
				return f.getClassDescription()==getClassDescription()&&f.getIndex()==getIndex();
			} else {
				return false;
			}
		}
		
		/**
		 * Logical equality. Two fields are equal if index and name and type are equal. This method is used by checking
		 * class type equality only
		 * 
		 * @param f the field to check
		 * @return <code>true</code> if the fields are the same
		 */
		protected boolean fieldEquals(Field f) {
			if(f.h==h) {
				return f.getIndex()==getIndex()&&f.getName().equals(getName())&&f.getType().equals(getType());
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Class access for interface type definition
	 * 
	 * @author notalexa
	 *
	 */
	public static class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory) {
			super(factory,getTypeDescription());
		}
		
		@Override
		public AccessibleObject newAccessible(AccessContext context) throws BaseException {
			return new DefaultAccessibleObject(this,new ClassTypeDefinition());
		}
		
		@Override
		public Object getField(Object o, int index) throws BaseException {
			ClassTypeDefinition def=(ClassTypeDefinition)o;
			switch(index) {
				case 0:List<ObjectType> types=def.getTypes();
                    return types.size()==0?null:types.toArray(new ObjectType[types.size()]);
				case 1:return def.enableObjectRefs?Boolean.TRUE:null;
				case 2:return def.extendible?Boolean.TRUE:null;
				case 3:return def.fields.length==0?null:def.fields;
				case 4:return def.methods.length==0?null:def.methods;
			}
			return null;
		}

		@Override
		public void setField(Object o, int index, Object v) throws BaseException {
			ClassTypeDefinition def=(ClassTypeDefinition)o;
			switch(index) {
				case 0:def.addTypes((ObjectType[])v);
					break;
				case 1:def.enableObjectRefs=(boolean)v;
					break;
                case 2:def.extendible=(boolean)v;
                    break;
				case 3:def.fields=(Field[])v;
					break;
				case 4:def.methods=(MethodTypeDefinition[])v;
					break;
			}
		}

		@Override
		public Access createFieldAccess(Field f) throws BaseException {
			switch(f.getIndex()) {
				case 0:return forArray(f.getType(),ObjectType[].class);
				case 3:return new ArrayTypeAccess(f.getType(),new FieldAccess(factory),Field[].class);
				case 4:return forArray(f.getType(),MethodTypeDefinition[].class);
			}
			return super.createFieldAccess(f);
		}
		
		public Object finish(Object o) {
			ClassTypeDefinition def=(ClassTypeDefinition)o;
			if(def.fields==null) {
			    def.fields=NO_FIELDS;
			}
			if(def.methods==null) {
			    def.methods=NO_METHODS;
			}
			def.calculateHash();
			return def.fix();
		}
	}
	
	/**
	 * Class access for interface type definition
	 * 
	 * @author notalexa
	 *
	 */
	public static class FieldAccess extends AbstractClassAccess implements Access {
		public FieldAccess(AccessFactory factory) {
			super(factory,Types.FIELD_TYPE);
		}
		@Override
		public AccessibleObject newAccessible(AccessContext context) throws BaseException {
			ClassTypeDefinition classType=context.castTo(ClassTypeDefinition.class);
			if(classType==null) {
				throw new BaseException(BaseException.BAD_REQUEST,"No enclosing class type found.");
			} else {
				return new DefaultAccessibleObject(this,classType.new Field());
			}
		}
		
		@Override
		public Object getField(Object o, int index) throws BaseException {
			Field def=(Field)o;
			switch(index) {
				case 0:return def.index;
				case 1:return def.name;
                case 2:return def.extendible?Boolean.TRUE:null;
                case 3:return def.optional?Boolean.TRUE:null;
				case 4:return def.type;
				case 5:return def.tags;
				case 6:return def.hints;
                case 7:return def.defaultValue;
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setField(Object o, int index, Object v) throws BaseException {
			Field def=(Field)o;
			switch(index) {
				case 0:def.index=(int)v;
					break;
				case 1:def.name=(String)v;
					break;
                case 2:def.extendible=(boolean)v;
                    break;
                case 3:def.optional=(boolean)v;
                    break;
				case 4:def.type=(TypeDefinition)v;
					break;
				case 5:def.tags=(Map<String,String>)v;
					break;
				case 6:def.hints=(Set<String>)v;
					break;
                case 7:def.defaultValue=v;
                    break;
			}
		}

		@Override
		public Access createFieldAccess(Field f) throws BaseException {
			switch(f.getIndex()) {
				case 5:return forMap(f.getType(),Map.class);
				case 6:return forCollection(f.getType(),Set.class);
			}
			return super.createFieldAccess(f);
		}
		
		public Object finish(Object o) {
			Field def=(Field)o;
			// Ensure consistency
			switch(def.type.getFlavour()) {
    			case PrimitiveType:def.extendible=false;
    			    break;
    			case InterfaceType:
    			case ClassType:def.defaultValue=null;
    			    break;
			    default:def.extendible=false;
			            def.defaultValue=null;
			}
			def.calculateHash();
			return def;
		}
	}
}
