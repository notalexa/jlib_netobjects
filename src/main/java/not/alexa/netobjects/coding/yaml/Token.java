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
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import not.alexa.netobjects.coding.yaml.Yaml.DefaultHandler;
import not.alexa.netobjects.coding.yaml.Yaml.Document;
import not.alexa.netobjects.coding.yaml.Yaml.Handler;

/**
 * Tokens are the basic elements of this YAML implementation and can occur in two different flavour.
 * <ul>
 * <li>Intermediate tokens are created by the parser and are not expected to survive the parsing process. Typical
 * examples are the key and value indicator tokens {@code ?} and {@code :}.
 * <li>Modifier tokens are tokens serving as attributes to nodes. The currently supported modifier tokens are {@link Type#Anchor}
 * for anchors (with first character {@code &amp;}) and {@link Type#Script} for scripts (an extension with first character {@code @}).
 * <li>Nodes, which are either directly created by the parser or created using the {@link Yaml.DefaultHandler}. We have:
 * <ul>
 * <li>{@link Type#Scalar} and {@link Type#Alias} which are directly created by a parser.
 * <li>{@link Type#Sequence} and {@link Type#Map} which are composed tokens created by the default handler.
 * <li>{@link Type#DecoratedToken} which is a node decorated by a modifier
 * </ul>
 * 
 * 
 * @author notalexa
 *
 */
public interface Token {
	/**
	 * 
	 * @return the type of this token
	 */
	public Type getType();
	
	/**
	 * 
	 * @return the value of this token. For conventions, see the descriptions of the different types.
	 */
	public default String getValue() {
		return toString();
	}
	
	/**
	 * 
	 * @return this token as a list. For tokens with a type different from {@link Type#Sequence}, the
	 * method returns a singleton list with this token as the element
	 */
	public default List<Token> getArray() {
		return Collections.singletonList(this);
	}
	
	/**
	 * @return this token as a list of map entries. For tokens with a type different from {@link Type#Map}, this
	 * method returns a singleton list with key the scalar {@code "."} and value the token itself. 
	 */
	public default List<Map.Entry<Token, Token>> getMapArray() {
		return Collections.singletonList(new MapToken.Item(new SimpleToken(Type.Scalar,null,"."), this));
	}

	/**
	 * Convention method for the important case where the key of maps is a scalar (or decorated scalar). 
	 * 
	 * 
	 * @return this token as a list of map entries. For tokens with a type different from {@link Type#Map}, this
	 * method returns a singleton map with key {@code "."} and value the token itself. 
	 * @throws if a key of the map is not a scalar (or decorated scalar)
	 */
	public default Map<String,Token> getMap() throws YamlException {
		return Collections.singletonMap(".",this);
	}
	
	/**
	 * Method for undecorating tokens. The modifier are put into a list of tokens while resolving the base token.
	 * If the token doesn't represent a token of type {@link Type#DecoratedToken}, the callback is evaluated with an
	 * empty list of modifiers.
	 * 
	 * @param callback the callback to consume the provided list of modifiers and the base token.
	 * 
	 */
	public default void undecorate(BiConsumer<List<Token>,Token> callback) {
		callback.accept(Collections.emptyList(), this);
	}

	/**
	 * Decorate this token with the given modifier.
	 * 
	 * @param modifier the modifier for this token
	 * @return a token representing the decorated node
	 * @throws YamlException if this token is not a node or the modifier has a wrong type 
	 */
	public default DecoratedToken decorate(Token modifier) throws YamlException {
		if(!isNode()) {
			throw new YamlException("Not a node: "+getType());
		}
		switch(modifier.getType()) {
			case Anchor:
			case Script:
				break;
			default:throw new YamlException("Not a modifier: "+modifier.getType());
		}
		return new DecoratedToken(modifier, this);
	}
	
	/**
	 * Decorate this token with all the modifiers. It is save to call this method on non node tokens with
	 * an empty modifier list.
	 * 
	 * @param modifiers the list of modifiers
	 * @return the decorated token
	 * @throws YamlException if this token is not a node and the list has modifiers is not empty or if one of the
	 * modifiers has a wrong type
	 */
	public default Token decorate(List<Token> modifiers) throws YamlException {
		Token result=this;
		if(modifiers.size()>0) for(int i=modifiers.size()-1;i>=0;i--) {
			result=result.decorate(modifiers.get(i));
		}
		return result;
	}
	
	
	/**
	 * Tokens representing a node can be viewed as {@link Document}'s.
	 * 
	 * @return this token as a document
	 * @throws YamlException if this token doesn't represent a document (that is is not a node)
	 */
	public default Yaml.Document asDocument() throws YamlException {
		if(!isNode()) {
			throw new YamlException("Not a document type: "+getType());
		}
		return new Yaml.Document() {
			@Override
			public <T extends Handler> T process(T handler) throws YamlException {
				handler.beginDocument();
				Yaml.process(Token.this, handler);
				handler.endDocument();
				return handler;
			}
		};
	}
	
	/**
	 * 
	 * @return if this token represents a node
	 */
	public default boolean isNode() {
		switch(getType()) {
			case DecoratedToken:
			case Alias:
			case Scalar:
			case Map:
			case Sequence: return true;
			default: return false;
		}
	}
	
	/**
	 * In YAML, nodes have a tag attribute (which can be empty).
	 * 
	 * @return the tag of this token
	 */
	public default String getTag() {
		return null;
	}

	/**
	 * The different types of a token.
	 * 
	 * @author notalexa
	 *
	 */
	public enum Type {

		/**
		 * Represents a string. Tokens of this type are generated by the parser and the value of the token
		 * is the string itself.
		 * {@link Token#isNode()} should return {@code true} for this type.
		 */
		Scalar,

		/**
		 * Represents a reference to an anchored node. Tokens of this type are generated by the parser and the value of
		 * the token is the string representation without the {@link *} (which is considered as a representation detail).
		 * {@link Token#isNode()} should return {@code true} for this type since they reference a node.
		 */
		Alias,
		
		/**
		 * Represents a key indicator (that is a {@code ?}). For internal use only.
		 */
		KeyIndicator,

		/**
		 * Represents a value indicotar (that is a {@code :}). For internal use only.
		 */
		ValueIndicator,

		/**
		 * Represents a map. The tag of this token is {@code !map} and this token is never generated by the parser but
		 * created using the {@link DefaultHandler}.
		 * {@link Token#isNode()} should return {@code true} for this type.
		 * 
		 * @see MapToken
		 * 
		 */
		Map,

		/**
		 * Represents a sequence (array, list). The tag of this token is {@code !seq} and this token is never generated by the parser but
		 * created using the {@link DefaultHandler}.
		 * {@link Token#isNode()} should return {@code true} for this type.
		 * 
		 * @see SequenceToken
		 */
		Sequence,

		/**
		 * Represents an anchor. This is a modifier token which is generated by the parser (and provided in the API as a
		 * part of the modifiers list). The value of
		 * the token is the string representation without the {@link &} (which is considered as a representation detail).
		 * {@link Token#isNode()} should return {@code false} for this type.
		 * 
		 */
		Anchor,
		
		/**
		 * Represents a script. This extension to standard YAML is a modifier token which is generated by the parser 
		 * (and evaluated directly in
		 * the processor but can be provided by explicitly creating a decorated token). The value of
		 * the token is the string representation without the {@link @} (which is considered as a representation detail).
		 * {@link Token#isNode()} should return {@code false} for this type.
		 */
		Script, 
		/**
		 * Represents a decorated token, that is a node with a modifier. The value of this node is of no use and therefore unspecified.
		 * 
		 * {@link Token#isNode()} should return {@code true} for this type.
		 * @see DecoratedToken
		 */
		DecoratedToken,
		/**
		 * Represents a curly open brace (that is a <code>{</code>). For internal use only.
		 */
		CurlyOpen,
		/**
		 * Represents a curly close brace (that is a <code>}</code>). For internal use only.
		 */
		CurlyClose,
		/**
		 * Represents a square open brace (that is a <code>[</code>). For internal use only.
		 */
		SquareOpen,
		/**
		 * Represents a square close brace (that is a <code>]</code>). For internal use only.
		 */
		SquareClose,
		/**
		 * Represents a separator (that is a <code>,</code>) in flow mode. For internal use only.
		 */
		Separator;
	}
	
	/**
	 * Implementation of {@link Token} for all types except {@link Type#Map}, {@link Type#Sequence} and
	 * {@link Type#DecoratedToken}. It is not forbidden to use these types in a {@code SimpleToken} but the
	 * behaviour is not what is expected.
	 * 
	 * @author notalexa
	 *
	 */
	public static class SimpleToken implements Token {
		private Type type;
		private String value;
		private String tag;
		SimpleToken(Type type,String tag,String value) {
			this.type=type;
			this.value=value;
			this.tag=tag;
		}
		
		@Override
		public Type getType() {
			return type;
		}
		
		@Override
		public String getValue() {
			switch(type) {
				case KeyIndicator:return "?";
				case ValueIndicator:return ":";
				default: return value;
			}
		}
		
		@Override
		public String toString() {
			String s;
			switch(type) {
			case Script:s="@"+value;
			break;
			case Alias:s="*"+value;
				break;
			case Anchor:s="&"+value;
				break;
			case KeyIndicator:return "?";
			case ValueIndicator:return ":";
			default:s=Yaml.encode(value,3);
			break;
			}
			return tag==null?s:("!"+tag+" "+s);
		}
		
		@Override
		public String getTag() {
			return tag;
		}
	}
	
	/**
	 * Implementation for {@link Type#DecoratedToken}.
	 * 
	 * @author notalexa
	 *
	 */
	public final static class DecoratedToken implements Token {
		private Token decorator;
		private Token value;
		private DecoratedToken(Token decorator,Token value) {
			this.decorator=decorator;
			this.value=value;
		}
		
		public Type getType() {
			return Type.DecoratedToken;
		}
		
		public Token getToken() {
			return value;
		}
		
		public Token getDecorator() {
			return decorator;
		}

		@Override
		public void undecorate(BiConsumer<List<Token>, Token> callback) {
			List<Token> modifier=new ArrayList<>();
			Token rover=this;
			while(rover.getType()==Type.DecoratedToken) {
				if(rover instanceof DecoratedToken) {
					DecoratedToken dt=(DecoratedToken)rover;
					modifier.add(dt.decorator);
					rover=dt.value;
				} else {
					throw new RuntimeException("Type 'DecoratedToken' requires class DecoratedToken");
				}
			}
			callback.accept(modifier, rover);
		}

		public String toString() {
			return decorator.toString()+" "+value.toString();
		}
		
		public String getValue() {
			return decorator.toString()+" "+value.getValue();
		}
	}
}
