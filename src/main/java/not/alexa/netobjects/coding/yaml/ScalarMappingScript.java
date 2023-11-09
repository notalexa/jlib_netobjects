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

import java.util.List;

import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Delegator;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;
import not.alexa.netobjects.utils.Mapper;
import not.alexa.netobjects.utils.PropertyExpander;

/**
 * A script which uses a (string to string) {@link Mapper} to map
 * scalars.
 * <br>A typical use case is reading in network objects which starts up a
 * server from a YAML file expanding system properties in construction.
 * 
 * @author notalexa
 *
 */
public class ScalarMappingScript implements YamlScript {
	private String name;
	private boolean expandKey=false;
	private Mapper<String,String,? extends Throwable> mapper;

	/**
	 * Construct a default script with name {@literal expand} and
	 * the {@link PropertyExpander} as mapper.
	 * 
	 */
	public ScalarMappingScript() {
		this("expand",false,new PropertyExpander());
	}
	
	/**
	 * Construct a script with the given mapper. Keys
	 * are not mapped.
	 * 
	 * @param name the name of the script
	 * @param mapper the mapper to use
	 */
	public ScalarMappingScript(String name,Mapper<String,String,? extends Throwable> mapper) {
		this(name,false,mapper);
	}

	/**
	 * Construct a script with the given parameters.
	 * 
	 * @param name the name of the script
	 * @param expandKey if {@code true}, keys are also expanded
	 * @param mapper the mapper to use
	 */
	public ScalarMappingScript(String name,boolean expandKey,Mapper<String,String,? extends Throwable> mapper) {
		this.mapper=mapper;
		this.expandKey=expandKey;
		this.name=name;
	}


	@Override
	public String getName() {
		return name;
	}

	@Override
	public Handler create(Yaml yaml, Handler base) throws YamlException {
		return new ExpandHandler(base);
	}

	private class ExpandHandler extends Delegator {
		
		private ExpandHandler(not.alexa.netobjects.coding.yaml.Yaml.Handler delegate) {
			super(delegate);
		}

		@Override
		public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
			if((!key||expandKey)&&token.getType()==Type.Scalar) try {
				String v=mapper.map(token.getValue());
				if(v!=token.getValue()) {
					token=new Token.SimpleToken(Type.Scalar,token.getTag(), v);
				}
			} catch(Throwable t) {
				YamlException.throwException(t);
			}
			super.scalar(key, modifier, token);
		}
	}
}
