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

/**
 * A type definition is just a collection of types and a flavour.
 * <br>Implementation Note: <b>Type definitions should never be used as
 * keys in hash tables.<b> The reason is simple: In general, it is not possible
 * to define a good hash key for a given type because of the special equality of
 * types. If we have three namespaces <code>ns1, ns2, ns3</code> with types <code>t1, t2, t3</code> the type definitions
 * with types <code>(t1,t2)</code>, <code>(t1,t3)</code> and <code>(t2,t3)</code> are 
 * equal but no suitable hash code can be defined.
 * 
 * 
 * @author notalexa
 *
 */
public abstract class TypeDefinition extends Namespace.Types {
	
	/**
	 * 
	 * @return the type definition of this type (this is an interface definition)
	 */
	public static TypeDefinition getTypeDescription() {
		return Types.TYPE;
	}
	
	/**
	 * Convenience method if we want to define a type for a given class.
	 * 
	 * @param clazz the clazz we define a type for
	 */
	protected TypeDefinition(Class<?> clazz) {
		this(ObjectType.createClassType(clazz));
	}
	
	/**
	 * Type definition for the given types
	 * @param types the types
	 */
	protected TypeDefinition(ObjectType...types) {
		addTypes(types);
	}
	
	protected boolean isImmutable() {
		return true;
	}
	
	public TypeDefinition fix() {
		return this;
	}
	
	public MethodTypeDefinition[] getMethods() {
	    return MethodTypeDefinition.NO_METHODS;
	}


	/**
	 * 
	 * @return the flavour of this type
	 */
	public abstract Flavour getFlavour();
	
	protected static int typeHash(TypeDefinition type) {
		if(type!=null) switch(type.getFlavour()) {
			case ArrayType:return 31*(typeHash(((ArrayTypeDefinition)type).getComponentType())^17);
			case EnumType:
			case PrimitiveType:
			case MethodType:return type.hashCode();
			// We cannot 
			case ClassType:
			case InterfaceType:
			default:return 0;
		} else {
			return 0;
		}
	}
	
	protected static int typeHash(TypeDefinition[] types) {
		int h=0;
		if(types!=null) for(TypeDefinition type:types) {
			h^=17*typeHash(type);
		}
		return h;
	}

}
