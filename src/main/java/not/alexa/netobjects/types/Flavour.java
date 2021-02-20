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
 * Simple enumeration of supported types.
 * 
 * @author notalexa
 *
 */
public enum Flavour {
	/**
	 * Type for an error condidition. If an {@link ObjectType} cannot be resolved, a type of this flavour can be created representing the
	 * (unknown) object type.
	 */
	UnknownType,
	
	/**
	 * Primitive types are expected to be "simple" and codeable in
	 * a simple way. All java primitive types are primitive in this
	 * framework but others like <code>byte[]</code> are primitiv too.
	 * @see PrimitiveTypeDescription
	 */
	PrimitiveType,
	
	/**
	 * Enumeration types are types with a restricted set of (string) values.
	 * @see EnumTypeDescription
	 */
	EnumType,
	
	/**
	 * Class types contains fields (or attributes) and methods.
	 * Class types can be devided into two categories:
	 * <ul>
	 * <li>Anonymous types have no <i>type names</i> and cannot
	 * be resolved directly. But they can occur as parts of other types.
	 * <li>Named types are resolveable via a type scheme. Prominent example
	 * of a type scheme is the Java class type scheme. Most classes have
	 * a name but not all. <code>ArrayList&lt;String%gt;</code> or inner types
	 * are example for anonymous types (in the first example, the
	 * generic type <code>ArrayList</code> has a name but the fully resolved
	 * type not.
	 * </ul>
	 * @see ClassTypeDescription
	 */
	ClassType,
	
	/**
	 * Interface types contains methods only. Class types can be
	 * plugged in into fields which fulfill the requirements of the
	 * interface type.
	 * <br>Interface types fall in two categories:
	 * <ul>
	 * <li>Restricted types list the allowed values of substitutes.
	 * <li>Unrestricted types don't list the allowed values. In
	 * principle all values can be substituted.
	 * </ul>
	 * 
	 * @see InterfaceTypeDescription
	 */
	InterfaceType,
	
	/**
	 * Array types represent arrays, or collections (list,set) of
	 * other types (including array types).
	 * 
	 * @see ArrayTypeDescription
	 */
	ArrayType,
	
	/**
	 * Method types represent abstract methods typically attached
	 * to a class of interface. In contrast to "normal" methods,
	 * return values are not single valued but represents an
	 * array of return values. If the length of the return vector is
	 * zero, the method is void and if it is zero or one, the method
	 * can be potentially linked to a (java) implementation.
	 * 
	 * @see MethodTypeDescription
	 */
	MethodType;
}
