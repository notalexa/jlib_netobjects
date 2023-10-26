/*
 * Copyright (C) 2023 Not Alexa
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
package not.alexa.netobjects.coding.yaml;

import not.alexa.netobjects.coding.yaml.Yaml.Handler;

/**
 * YAML scripts are an extension of this implementation to YAML. Roughly, a
 * YAML script takes a node and transforms this into another node. In a representation, YAML scripts
 * can be defined at any point in which anchors are allowed and are recognized by {@code @}.
 * 
 * In this implementation, requested scripts are required. This implies, that any YAML file with undefined scripts are rejected.
 * In the YAML case {@link Yaml.Mode#Json}, all provided scripts provided and therefore
 * any document containing script information is rejected.
 * 
 * Scripts are especially useful for configuration. For example, the {@link IncludeScript} takes a scalar (or an array of [array of...] scalars) and
 * includes them into the current script as a YAML object. Other scripts (modifying scalars with environment variables for example) are of course
 * possible.
 * <p>From a technical point of view, a script is a factory for a filter for {@link Handler}'s. The script takes input from the document and generates
 * input for the filterd {@link Handler}.
 * 
 * @author notalexa
 *
 */
public interface YamlScript {
	/**
	 * The Identity Script for convenience. The name of this script is surprisingly {@¢ode identity}.
	 */
	public static final YamlScript IDENTITY=new YamlScript() {
		
		@Override
		public String getName() {
			return "identity";
		}
	};
	
	/**
	 * 
	 * @return the name of this script.
	 */
	public String getName();

	/**
	 * Create a filter for the (outgoing) event handler.
	 * 
	 * @param yaml the yaml configuration
	 * @param base the base handler which needs to be filtered by this script
	 * @return the filtered handler
	 * @throws YamlException if an error occurs
	 */
	public default Handler create(Yaml yaml,Handler base) throws YamlException {
		return base;
	}	
}
