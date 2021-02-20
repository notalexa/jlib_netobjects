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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract type for interfaces and classes. Types based on this class are allowed to reference them self if and only if they are not
 * {@link TypeDefinition#isAnonymous()}.
 * 
 * @author notalexa
 *
 */
public abstract class AbstractClassTypeDefinition extends TypeDefinition {
	private static final MethodTypeDefinition[] NO_METHODS=new MethodTypeDefinition[0];
	private boolean immutable;
	protected int h;
	protected InterfaceTypeDefinition[] interfaces;
	protected MethodTypeDefinition[] methods=NO_METHODS;
	protected MethodTypeDefinition[] allMethods=NO_METHODS;

	protected AbstractClassTypeDefinition(Class<?> clazz) {
		super(clazz);
	}

	public AbstractClassTypeDefinition(ObjectType... types) {
		super(types);
	}
	
	public MethodTypeDefinition[] getMethods() {
		return allMethods;
	}

	@Override
	protected boolean isImmutable() {
		return immutable;
	}
	
	@Override
	public synchronized TypeDefinition fix() {
		immutable=true;
		return this;
	}
	
	protected abstract void calculateHash();
	
	public int hashCode() {
		return h;
	}
	
	public boolean equals(Object o) {
		if(o.getClass().equals(getClass())&&o.hashCode()==hashCode()) {
			AbstractClassTypeDefinition other=(AbstractClassTypeDefinition)o;
			if(isAnonymous()) {
				if(other.isAnonymous()) {
					return deepEquals(other);
				} else {
					return false;
				}
			} else if(!other.isAnonymous()) {
				if(check(other)==0) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected boolean deepEquals(AbstractClassTypeDefinition other) {
		return Arrays.equals(other.methods,methods);
	}

	/**
	 * Add interfaces to this class type declaration. This interfaces are <b>not part of the global class definition</b> but can be 
	 * defined in a <b>local execution environment</b> and are therefore non persistent.
	 * 
	 * @param interfaces the interfaces to add
	 * @return this type description
	 */
	public synchronized AbstractClassTypeDefinition addInterface(InterfaceTypeDefinition...interfaces) {
		if(this.interfaces==null) {
			this.interfaces=interfaces.length==0?null:interfaces;
		} else {
			InterfaceTypeDefinition[] tmp=this.interfaces;
			outerloop:for(InterfaceTypeDefinition i:interfaces) {
				if(i==null) {
					continue outerloop;
				}
				for(InterfaceTypeDefinition i1:this.interfaces) {
					if(i1.equals(i)) {
						continue outerloop;
					}
				}
				tmp=Arrays.copyOf(tmp,tmp.length+1);
				tmp[tmp.length-1]=i;
			}
			this.interfaces=tmp;
		}
		if(this.interfaces!=null) {
			allMethods=computeMethodArray(Arrays.asList(methods),this.interfaces);
		}
		return this;
	}
	
	private MethodTypeDefinition[] computeMethodArray(List<MethodTypeDefinition> methodTypes,InterfaceTypeDefinition[] interfaces) {
		Set<MethodTypeDefinition> methods=new HashSet<MethodTypeDefinition>(methodTypes);
		if(interfaces!=null) for(InterfaceTypeDefinition i:interfaces) {
			for(MethodTypeDefinition methodType:i.getMethods()) {
				methods.add(methodType.forClass(this));
			}
		}
		return methods.toArray(new MethodTypeDefinition[methods.size()]);
	}

	protected abstract class Builder<T extends Builder<T>> {
		protected List<MethodTypeDefinition> methods=new ArrayList<MethodTypeDefinition>();
		
		/**
		 * Create a new method for this type
		 * 
		 * @param name the name of the method.
		 * @return a builder for additional configuration
		 */
		public MethodBuilder createMethod(String name) {
			return new MethodBuilder(name);
		}

		/**
		 * After creation, the type is immutable.
		 * 
		 * @return the type definition build from this builder.
		 */
		public AbstractClassTypeDefinition build() {
			synchronized (AbstractClassTypeDefinition.this) {
				if(!immutable) {
					immutable=true;
					allMethods=AbstractClassTypeDefinition.this.methods=computeMethodArray(methods,null);
				}
				return AbstractClassTypeDefinition.this;
			}
		}
		
		/**
		 * Add all methods of all interfaces to this type definition. This methods become part of the global definition.
		 * 
		 * @param interfaces the interfaces to add
		 * @return this for additional configuration
		 */
		public T addInterface(InterfaceTypeDefinition...interfaces) {
			for(InterfaceTypeDefinition i:interfaces) {
				for(MethodTypeDefinition m:i.getMethods()) {
					methods.add(m);
				}
			}
			return that();
		}
		
		/**
		 * 
		 * @return <code>true</code> if the underlying type is already immutable
		 */
		public boolean isImmutable() {
			return AbstractClassTypeDefinition.this.isImmutable();
		}
		
		protected abstract T that();
		
		public class MethodBuilder {
			private MethodTypeDefinition.Builder builder;
			
			private MethodBuilder(String name) {
				builder=new MethodTypeDefinition(AbstractClassTypeDefinition.this,name).createBuilder();
			}
			/**
			 * 
			 * @return a method type definition build from the builders material
			 */
			public T build() {
				methods.add(builder.build());
				return that();
			}
			
			/**
			 * Modify the parameter types of this method
			 * @param parameterTypes the new parameter types
			 * @return <code>this</code>
			 */
			public MethodBuilder setParameterTypes(TypeDefinition...parameterTypes) {
				builder.setParameterTypes(parameterTypes);
				return this;
			}

			/**
			 * Modify the return types of this method
			 * @param returnTypes the new return types
			 * @return <code>this</code>
			 */
			public MethodBuilder setReturnTypes(TypeDefinition...returnTypes) {
				builder.setReturnTypes(returnTypes);
				return this;
			}
		}
	}
}
