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
package not.alexa.netobjects.coding.json;

import not.alexa.netobjects.coding.yaml.Yaml;
import not.alexa.netobjects.coding.yaml.Yaml.Mode;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;

/**
 * Coding scheme for JSON. Since this library handles JSON as a subset of YAML, the package
 * is almost empty and the implementation is done in the YAML package. The following
 * default schemata are defined:
 * <ul>
 * <li>The {@link #DEFAULT_SCHEME} uses the {@link Mode#ExtendedJson} mode. Therefore,
 * anchors and alias are allowed.
 * <li>The {@link #REST_SCHEME} uses the {@link Mode#Json} mode. Therefore, anchors and
 * alias are not allowed. This implies that some objects cannot be serialized (or deserialized)
 * with this scheme. No root object is set.
 * <li>The {@link #RESTRICTED_SCHEME} uses the {@link Mode#Json} mode. Therefore, anchors and
 * alias are not allowed. This implies that some objects cannot be serialized (or deserialized)
 * with this scheme.
 * </ul>
 * @author notalexa
 * @see YamlCodingScheme
 * @see Yaml
 * @see Mode
 */
public class JsonCodingScheme {
	
	/**
	 * The default scheme.
	 */
    public static final YamlCodingScheme DEFAULT_SCHEME=new YamlCodingScheme(new Yaml(Mode.ExtendedJson)).newBuilder().setRootType(Object.class).addInlineKeys(String.class).build();
    
    /**
     * The restricted scheme for rest (that is without a root type)
     */
    public static final YamlCodingScheme REST_SCHEME=new YamlCodingScheme(new Yaml(Mode.Json)).newBuilder().addInlineKeys(String.class).addDefaultTags().build();
    
    /**
     * The restricted scheme. 
     */
    public static final YamlCodingScheme RESTRICTED_SCHEME=REST_SCHEME.newBuilder().setRootType(Object.class).build();
}