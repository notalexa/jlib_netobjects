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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Token implementation for {@link Type.Map}.
 * 
 * @author notalexa
 *
 */
public class MapToken implements Token {
	protected List<Entry<Token, Token>> items=new ArrayList<>();
	@Override
	public Type getType() {
		return Type.Map;
	}

	
	public void add(Token key,Token value) {
		items.add(new Item(key,value));
	}
	
	@Override
	public String getTag() {
		return "!map";
	}

	public String indented(Token token) {
		String val=token.toString();
		return val.replace("\n","\n    ").trim();
	}
	
	@Override
	public String toString() {
		StringBuilder builder=new StringBuilder();
		builder.append('!').append(getTag()).append(" {\n");
		for(Map.Entry<Token,Token> item:items) {
			builder.append("  ? ").append(indented(item.getKey())).append('\n');
			builder.append("  : ").append(indented(item.getValue())).append('\n');
		}
		return builder.append("}").toString();
	}

	
	@Override
	public List<Entry<Token, Token>> getMapArray() {
		return items;
	}


	@Override
	public Map<String, Token> getMap() throws YamlException {
		if(items.isEmpty()) {
			return Collections.emptyMap();
		} else {
			Map<String,Token> result=new HashMap<>();
			for(Entry<Token,Token> entry:items) {
				if(entry.getKey().getType()!=Type.Scalar) {
					throw new YamlException("Unexpected non scalar key of type "+entry.getKey().getType()+" detected.");
				}
				result.put(entry.getKey().getValue(),entry.getValue());
			}
			return result;
		}
	}


	public static class Item implements Map.Entry<Token,Token> {
		Token key;
		Token value;
		Item(Token key,Token value) {
			this.key=key;
			this.value=value;
		}
		
		@Override
		public Token getKey() {
			return key;
		}
		
		@Override
		public Token getValue() {
			return value;
		}

		@Override
		public Token setValue(Token value) {
			Token saved=this.value;
			this.value=value;
			return saved;
		}		
	}
}
