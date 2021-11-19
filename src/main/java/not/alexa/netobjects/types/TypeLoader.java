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

import java.util.Arrays;
import java.util.Collection;

import not.alexa.netobjects.api.Final;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.utils.OverlayTypeLoader;

/**
 * A type loader is responsible for maintaining types
 * of network objects and can be compared with a class loader
 * for java classes.
 * <br>Beside supporting type descriptions for a given type,
 * the loader also maintains it's own class loader and is
 * responsible for <i>linking type descriptions to local (java)
 * classes </i>.
 * Since any {@link not.alexa.netobjects.Context} has a type
 * loader, the loader (and therefore the linkage) depends on
 * the specific context of evaluation.
 * <br>A type resolver supporting explicitly defined types using
 * the builder mechanism of {@link ClassTypeDefinition} is 
 * {@link DefaultTypeLoader}. More complex loaders
 * will follow in subsequent libraries. Possible mechanism of
 * providing such types are:
 * <ul>
 * <li>Reflection and annotations as in JAXB
 * <li>Transforming from other representations like XML schemata
 * or IDLs
 * <ul>
 * 
 * @author notalexa
 * @see not.alexa.netobjects.Context
 * @see DefaultTypeLoader
 */
public interface TypeLoader {
	/**
	 * Not very useful but suitable for initialization and
	 * default behaviour.
	 *  
	 */
	public final TypeLoader DEFAULT_LOADER=new TypeLoader() {
	};
	
	/**
	 * 
	 * @return the class loader attached to this type loader
	 */
	public default ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}
	
	/**
	 * Resolve the given type. Note that different
	 * types may result in the same description. For example,
	 * a description may be represented by
	 * <ul>
	 * <li>A java class name
	 * <li>A .NET class name
	 * <li>A type schema name similar to an XML namespace
	 * <li>An object identifier
	 * </ul>
	 * The default implementation is not very useful and
	 * provides a type description for primitive types.
	 * 
	 * @param t the type
	 * @return the corresponding type description
	 * 
	 */
	public default TypeDefinition resolveType(ObjectType t) {
		return PrimitiveTypeDefinition.getTypeDescription(t);
	}

	/**
	 * Convenience method. Search for the corresponding {@link Type},
	 * 
	 * @param clazz the class 
	 * @return the corresponding type definition
	 */
	public default TypeDefinition resolveType(Class<?> clazz) {
		return resolveType(ObjectType.createClassType(clazz));
	}

	/**
	 * This method is the core method for linking network objects to java types. Whenever a server provides some functionality implementing
	 * a {@link MethodTypeDefinition}, the method is called on a network object using a suitable {@link AccessibleObject#call(not.alexa.netobjects.Context, AccessibleObject...)}.
	 * The server resolves the underlying network object (which is in the call) and links it to a local (java) class. This class <b>which
	 * depends on the network object and the calling context</b> links the network method to a local implementation on the local java class and
	 * evaluates it. The result (and the underlying network object if side effects has to be taken into account) are transfered back to
	 * the client and that's it: The client gets a result without any knowledge of any underlying representiation.
	 * <br>The default implementation assumes a sorted array of types returned by {@link TypeDefinition#getTypes()} and examines the first
	 * one. If this is a class, the class is returned. If this is a {@link JavaClass} type, the class is resolved with the current class loader.
	 * Otherwise the network type is not linked to a local class.
	 * <br>In the default implementation the type is resolved using the class type of the type definition (and <code>null</code> if the class type is not present which implies that
	 * no local base type is present for the type definition.
	 * 
	 * @param type the type we need a link for
	 * @return the corresponding class or <code>null</code> if the type is not linked.
	 */
	public default Class<?> getLinkedLocal(TypeDefinition type) {
		return type==null?null:getLinkedLocal(type.getJavaClassType());
	}
	
	/**
	 * Resolve the class for a {@link JavaClass.Type}. This can be the type itself or an overlay defined in this type loader.
	 * 
	 * @param type the type to resolve
	 * @return the corresponding class as defined in this loader
	 */
    public default Class<?> getLinkedLocal(Type type) {
        return type==null?null:type.asClass(getClassLoader());
    }
	
	/**
	 * Define the classes as overlays. Classes which doesn't define an overlay or classes which define an
	 * overlay for a base class which is defined as {@link Final} <b>must</b> be ignored.
	 * 
	 * @param overlays the overlays
     * @return The type loader with the given overlays. 
	 */
	public default TypeLoader overlay(Class<?>...overlays) {
        return overlay(Arrays.asList(overlays));
	}
	
	/**
	 * 
     * Define the classes as overlays. Classes which doesn't define an overlay or classes which define an
     * overlay for a base class which is defined as {@link Final} <b>must</b> be ignored.
     * 
	 * @param overlays the overlays
     * @return The type loader with the given overlays. 
	 */
    public default TypeLoader overlay(Collection<Class<?>> overlays) {
        return new OverlayTypeLoader(this, overlays);
    }    
}
