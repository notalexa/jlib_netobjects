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
 * Inside the network object framework, types are represented by
 * object types. Each object type has a namespace it belongs to and two
 * objects types are equals if and only if the {@link #getUrn()} method returns
 * the same result. Since the part identifying the namespace is the same for
 * two object types from the same namespace, this implies that two objects 
 * in the same namespace are equal if and only if the urn part with namespace
 * information stripped ({@link #getName()} is the same. 
 * <p>This type is an interface but nevertheless it is <i>defined as a primitive
 * type</i>. Therefore, the system requires a unique string representation
 * inside the object type universe which is of course the {@link #getUrn()} value and
 * the corresponding resolution is the {@link #resolve(String)} method defined
 * here as a static method.
 * 
 * @author notalexa
 *
 */
public interface ObjectType {
	
	/**
	 * Resolve the given urn.
	 * @param urn the urn to resolve
	 * @return the corresponding object type
	 */
	public static ObjectType resolve(String urn) {
		return Namespace.resolve(urn);
	}
	
	/**
	 * Create an object type in the normal Java Class Namespace representing the given class. Separated by colon, a version can optionally appended.
	 * 
	 * @param className the class name of the type
	 * @return an object type representing the java class
	 */
	public static ObjectType createClassType(String className) {
		return Namespace.getJavaNamespace().create(className);
	}
	
	/**
	 * Convenience method returning a type for the given class in the default version.
	 * 
	 * @param clazz the class we need a type for
	 * @return the type representing the class
	 */
	public static ObjectType createClassType(Class<?> clazz) {
		return createClassType(Namespace.asString(clazz));
	}

	
	/**
	 * 
	 * @return the namespace of this object type
	 */
	public Namespace getNamespace();
	
	/**
	 * 
	 * @return the stripped urn (without namespace information) of this type
	 */
	public String getName();
	
	/**
	 * For any type <code>type</code>, we know
	 * <pre>
	 * true==type.equals(ObjectType.resolve(type.getUrn()));
	 * </pre>
	 * 
	 * @return the complete urn of this object type.
	 * 
	 */
	public String getUrn();
}
