/*
 * Copyright (C) 2021 Not Alexa
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
package not.alexa.netobjects.coding.xml;

/**
 * Helper class to properly encode strings for usage as an XML attribute or text.
 * 
 * @author notalexa
 *
 */
public class XMLHelper {

	private XMLHelper() {
	}

	/**
	 * Encode the string for usage in an attribute.
	 * 
	 * @param s the string to encode
	 * @return the encoded string
	 */
	public static String attribute(String s) {
		return encode(true,s);
	}
	
	/**
	 * Encode the string for usage in an XML text node.
	 * 
	 * @param s the string to encode
	 * @return the encoded string
	 */
	public static String text(String s) {
		return encode(false,s);
	}
	
	/**
	 * Encode the string for usage in either an attribute node or text node.
	 * 
	 * @param asAttribute encode for an attribute node if <code>true</code>
	 * @param s the string to encode
	 * @return the encoded string
	 */
	public static String encode(boolean asAttribute,String s) {
		if(s!=null&&s.length()>0) {
			char[] chars=s.toCharArray();
			String entity=null;
			StringBuilder builder=null;
			int k=0;
			final int n=chars.length;
			for(int i=0;i<n;i++) {
				switch(chars[i]) {
					case '&':entity="&amp;"; break;
					case '<':entity="&lt;"; break;
					case '>':entity="&gt;"; break;
					case '"':entity=asAttribute?"&quot;":null; break;
					case '\r':entity=asAttribute?"&#xd;":null; break;
					case '\n':entity=asAttribute?"&#xa;":null; break;
					default:continue;
				}
				if(entity!=null) {
					if(builder==null) {
						builder=new StringBuilder(n+20);
					}
					builder.append(chars,k,i-k);
					builder.append(entity);
					k=i+1;
					entity=null;
				}
			}
			if(builder!=null) {
				builder.append(chars,k,n-k);
				return builder.toString();
			}
		}
		return s;
	}
}
