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
import java.util.List;

/**
 * Token implementation for {@link Type.Sequence}.
 * 
 * @author notalexa
 *
 */
public class SequenceToken implements Token {
	protected List<Token> items=new ArrayList<>();
	@Override
	public Type getType() {
		return Type.Sequence;
	}
	
	public void add(Token t) {
		items.add(t);
	}

	@Override
	public String getTag() {
		return "!seq";
	}
	
	@Override
	public List<Token> getArray() {
		return items;
	}

	private String indented(Token token) {
		String val=token.toString();
		return "  "+val.replace("\n","\n  ").trim();
	}
	
	@Override
	public String toString() {
		StringBuilder builder=new StringBuilder();
		builder.append('!').append(getTag()).append(" [\n");
		for(Token item:items) {
			builder.append(indented(item)).append('\n');
		}
		return builder.append("]").toString();
	}

}
