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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.ArrayTypeAccess;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;
import not.alexa.netobjects.utils.Matcher;

/**
 * An interface is a placeholder for a set of other types (implementing the interface). By default, interface types doesn't restrict
 * to concrete implementations but if necessary, a set of allowed types can be configured (and checked via the {@link Matcher#matches(Object)}
 * method).
 * 
 * @author notalexa
 *
 */
public class InterfaceTypeDefinition extends AbstractClassTypeDefinition implements Matcher<TypeDefinition> {
	public static ClassTypeDefinition getTypeDescription() {
		return Types.INTERFACE_TYPE;
	}
	protected Set<ObjectType> implementors;
	
	protected InterfaceTypeDefinition() {}
	
	public InterfaceTypeDefinition(ObjectType... implementors) {
		super();
		this.implementors=implementors.length>0?new HashSet<>(Arrays.asList(implementors)):null;
	}
	
	public InterfaceTypeDefinition(Class<?> clazz,ObjectType... implementors) {
		super(clazz);
		this.implementors=implementors.length>0?new HashSet<>(Arrays.asList(implementors)):null;
	}
	
	/**
	 * @return the constant {@link Flavour#InterfaceType}
	 */
	@Override
	public Flavour getFlavour() {
		return Flavour.InterfaceType;
	}

	/**
	 * 
	 * @return <code>true</code> if this interface type is restricted to a set of implementors
	 */
	public boolean hasImplementors() {
		return implementors!=null;
	}
	
	/**
	 * 
	 * @return the set of implementors
	 */
	public Set<ObjectType> getImplementors() {
		return implementors==null?Collections.emptySet():implementors;
	}

	@Override
	public boolean matches(TypeDefinition t) {
		if(implementors==null) {
			return true;
		}
		for(ObjectType type:t) {
			if(implementors.contains(type)) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Add interfaces to this class type declaration. This interfaces are <b>not part of the global class definition</b> but can be 
	 * defined in a <b>local execution environment</b> and are therefore non persistent.
	 * 
	 * @param interfaces the interfaces to add
	 * @return this type description
	 */
	public InterfaceTypeDefinition addInterface(InterfaceTypeDefinition...interfaces) {
		super.addInterface(interfaces);
		return this;
	}
	
	/**
	 * Build an interface type with the created builder if the underlying type is immutable results in the unmodified interface type.
	 * 
	 * @return a new builder
	 */
	public InterfaceBuilder createBuilder() {
		return new InterfaceBuilder();
	}
	

	@Override
	protected void calculateHash() {
		h=typeHash(methods);
		if(implementors!=null) {
			h^=implementors.hashCode();
		}
	}
	
	@Override
	protected boolean deepEquals(AbstractClassTypeDefinition other) {
		if(super.deepEquals(other)) {
			Set<ObjectType> otherImplementors=((InterfaceTypeDefinition)other).implementors;
			if(otherImplementors==null) {
				return implementors==null;
			} else if(implementors!=null) {
				return implementors.equals(otherImplementors);
			}
		}
		return false;
	}
	
	/**
	 * Builder class to configure the underlying interface type
	 * 
	 * @author notalexa
	 *
	 */
	public class InterfaceBuilder extends Builder<InterfaceBuilder> {
		protected Set<ObjectType> allowedTypes=new HashSet<ObjectType>();
		
		@Override
		protected InterfaceBuilder that() {
			return this;
		}
		
		/**
		 * Add the implementation type to the interface definition
		 * @param type the allowed type
		 * @return the builder for additional configuration
		 */
		public InterfaceBuilder addType(ObjectType type) {
			allowedTypes.add(type);
			return this;
		}
		
		/**
		 * Configure the underlying type if not immutable and return it
		 */
		public InterfaceTypeDefinition build() {
			if(!isImmutable()) {
				super.build();
				if(allowedTypes.size()>0) {
					implementors=Collections.unmodifiableSet(allowedTypes);
				} else {
					implementors=null;
				}
				calculateHash();
			}
			return InterfaceTypeDefinition.this;
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
		public AccessibleObject newInstance(AccessContext context) throws BaseException {
			return new DefaultAccessibleObject(this,new InterfaceTypeDefinition());
		}
		
		@Override
		public Object getField(Object o, int index) throws BaseException {
			InterfaceTypeDefinition def=(InterfaceTypeDefinition)o;
			switch(index) {
				case 0:List<ObjectType> types=def.getTypes();
                    return types.size()==0?null:types.toArray(new ObjectType[types.size()]);
				case 1:return def.implementors;
				case 2:return def.methods.length==0?null:def.methods;
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setField(Object o, int index, Object v) throws BaseException {
			InterfaceTypeDefinition def=(InterfaceTypeDefinition)o;
			switch(index) {
				case 0:def.addTypes((ObjectType[])v);
					break;
				case 1:def.implementors=(Set<ObjectType>)v;
					break;
				case 2:def.methods=(MethodTypeDefinition[])v;
					break;
			}
		}

		@Override
		public Access createFieldAccess(Field f) throws BaseException {
			switch(f.getIndex()) {
				case 0:return forArray(f.getType(),ObjectType[].class);
				case 1:return new ArrayTypeAccess(f.getType(),factory.resolve(this, PrimitiveTypeDefinition.getTypeDescription(ObjectType.class)),Set.class);
				case 2:return forArray(f.getType(),MethodTypeDefinition[].class);
			}
			return super.createFieldAccess(f);
		}
		
		public Object finish(Object o) {
			InterfaceTypeDefinition def=(InterfaceTypeDefinition)o;
			def.h=typeHash(def.methods);
			if(def.implementors!=null) {
				def.implementors=Collections.unmodifiableSet(def.implementors);
				def.h^=def.implementors.hashCode();
			}
			return def.fix();
		}
	}
}
