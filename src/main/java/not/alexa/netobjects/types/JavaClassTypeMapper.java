/*
 * Copyright (C) 2024 Not Alexa
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
 * Basic interface mediating between to (java) types. Extensions typically define filters from and to the
 * the source class to the coding class and will be installed in <i>some</i> environment.
 * <p>Mappers can be defined in files {@code META-INF/typemappers} (resolvable via the application class loader) 
 * and are automatically installed in the system.
 * 
 * @param <S> the source type
 * @param <C> the target type
 */
public interface JavaClassTypeMapper<S,C> {
	/**
	 * 
	 * @return the coding class type
	 */
	public Class<C> getCodingClass();
	
	/**
	 * 
	 * @return the source class type
	 */
	public Class<S> getSourceClass();
	
	/**
	 * Install this mapper in the current environment.
	 * 
	 */
	public void install();
}
