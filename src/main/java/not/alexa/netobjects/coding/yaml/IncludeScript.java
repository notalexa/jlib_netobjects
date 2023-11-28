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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import not.alexa.netobjects.coding.yaml.Token.Type;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;
import not.alexa.netobjects.utils.BaseUtils;

/**
 * Script which allows scalars and arrays of [arrays of...] scalars as input. Each scalar is interpreted as an URL pointing to
 * a YAML file. This file is included in the parsing input stream (with the same YAML setup).
 * 
 * A special protocol is recognized. URL's starting with {@code cp://} are considered to reference a resource in the class path and
 * are tried to resolved using the provided class loader. If not present, an exception is thrown.
 * 
 * @author notalexa
 *
 */
public class IncludeScript implements YamlScript {
	private String name;
	private ClassLoader loader;
	
	/**
	 * Constructs the default include script with name {@¢ode @include} and the class loader of this class.
	 */
	public IncludeScript() {
		this("include");
	}
	
	/**
	 * Uses the class loader of this class.
	 * 
	 * @param name the name of this script (without {@code @})
	 */
	public IncludeScript(String name) {
		this(name,IncludeScript.class.getClassLoader());
	}
	
	/**
	 * 
	 * @param name the name of this script (without {@code @})
	 * @param loader the class loader to use for {@code cp://} URLs
	 */
	public IncludeScript(String name,ClassLoader loader) {
		this.name=name;
		this.loader=loader;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public Handler create(Yaml yaml, Handler base) throws YamlException {
		return new IncludeHandler(yaml,base);
	}
	
	private class IncludeHandler implements Handler {
		private Handler delegate;
		private Yaml yaml;
		private IncludeHandler(Yaml yaml,Handler delegate) {
			this.yaml=yaml;
			this.delegate=delegate;
		}
		

		@Override
		public void beginObject(boolean key,List<Token> modifier) throws YamlException {
			throw new YamlException("Objects are forbidden in include scripts");
		}

		@Override
		public void endObject(boolean key) {
		}

		@Override
		public void beginArray(boolean key,List<Token> modifier) {
			// Ignore.
		}

		@Override
		public void endArray(boolean key) {
		}


		@Override
		public void scalar(boolean key, List<Token> modifier, Token token) throws YamlException {
			if(token.getType()==Type.Scalar) {
				String script=token.getValue();
				try(InputStream in=BaseUtils.resolve(loader, script)) {
					if(in!=null) {
						yaml.parse(in,new HandlerWrapper(delegate));
					} else {
						YamlException.throwException(new FileNotFoundException(script));
					}
				} catch(IOException e) {
					YamlException.throwException(e);
				}
			} else {
				throw new YamlException("Forbidden type in include scripts: "+token.getType());
			}
		}
	}

	private static class HandlerWrapper extends Yaml.Delegator implements Handler {
		private HandlerWrapper(Handler delegate) {
			super(delegate);
		}

		@Override
		public void beginDocument() {
		}

		@Override
		public void endDocument() {
		}
	}
}
