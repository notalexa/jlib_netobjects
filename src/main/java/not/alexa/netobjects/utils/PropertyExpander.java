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
package not.alexa.netobjects.utils;

import java.util.Collections;
import java.util.Map;

/**
 * The mapper expands property keys into values. In it's simplest form, the mapper expands each <code>${key}</code> occurence by
 * the corresponding value from a map. It's possible to include the system properties and/or environment variables into this expansion
 * process.
 * If the key is not mapped, an illegal argument exception is thrown. To include <code>${</code> the sequence must be quoted with <code>\${</code>.
 * Every non matching <code>}</code> is just copied into the output.
 * <p>In a more complicated version, default values for non exisiting keys can be specified. If the key is followed by a {@literal :}, the text between <code>:</code>
 * and <code>}</code> (including possible expansions) is considered as the replacement value if the key doesn't exist. In the default value, <code>${</code> must 
 * be quoted as before. To allow <code>}</code> in defaults, the following notation can be used: if a digit in square brackets is followed immediately after the colon,
 * the digit denotes the number of curly close brackets in the default value (without the number of <code>}</code> of default values of inner replacements). Therefore,
 * the default value in <code>${key:\${xxx}}</code> is <code>${xxx</code> while the default value in <code>${key:[1]\${xxx}}</code> is <code>${xxx}</code> (most likely
 * what you wanted).
 * 
 * @author notalexa
 *
 */
public class PropertyExpander implements Mapper<String,String,IllegalArgumentException> {
	/**
	 * Flag indicating that system properties should be included.
	 */
	public static final int FLAG_INCLUDE_SYSTEMPROPERTIES=1;
	/**
	 * Flag indicating that environment variables should be included.
	 */
	public static final int FLAG_INCLUDE_ENV=2;
	
	private final Map<String,String> props;
	private final int flags;

	/**
	 * Construct an expander with an empty map and system properties and environment variables enabled.
	 */
	public PropertyExpander() {
		this(FLAG_INCLUDE_ENV|FLAG_INCLUDE_SYSTEMPROPERTIES,Collections.emptyMap());
	}
	
	/**
	 * General mechanism to construct an expander
	 * 
	 * @param flags indicates if system properties and/or environment variables should be included. If {@link #FLAG_INCLUDE_SYSTEMPROPERTIES} is set, system properties
	 * are included, if {@link #FLAG_INCLUDE_ENV} is set, environment variables are included.
	 * 
	 * @param props the properties provided by this instance
	 */
	public PropertyExpander(int flags,Map<String,String> props) {
		this.props=props;
		this.flags=flags;
	}
	
	protected String resolve(String prop) {
		String result=props.get(prop);
		if(result==null) {
			result=FLAG_INCLUDE_SYSTEMPROPERTIES==(flags&FLAG_INCLUDE_SYSTEMPROPERTIES)?System.getProperty(prop):null;
		}
		if(result==null) {
			result=FLAG_INCLUDE_ENV==(flags&FLAG_INCLUDE_ENV)?System.getenv(prop):null;
		}
		return result;
	}
	
	private int map(String k,int offset,int nextOpener,int curlyCount,StringBuilder builder) {
		int closure=offset-1;
		while(true) {
			if(nextOpener<0) {
				nextOpener=Integer.MAX_VALUE;
			} else if(nextOpener>0&&k.charAt(nextOpener-1)=='\\') {
				if(builder!=null) {
					builder.append(k,offset,nextOpener-1);
				}
				offset=nextOpener;
				nextOpener=k.indexOf("${",offset+1);
				continue;
			}
			while(true) {
				closure=k.indexOf('}',closure+1);
				if(closure<0) {
					closure=k.length();
					curlyCount=0;
				}
				if(curlyCount>0&&closure<nextOpener) {
					curlyCount--;
				} else {
					break;
				}
			}
			if(nextOpener>closure) {
				if(builder!=null) {
					builder.append(k,offset,Math.min(closure,k.length()));
				}
				return Math.min(k.length(), closure+1);
			}
			if(true) {
				int opener=nextOpener;
				if(builder!=null) {
					builder.append(k,offset,nextOpener);
				}
				offset=nextOpener+2;
				int colon=k.indexOf(':',offset);
				nextOpener=k.indexOf("${",offset);
				if(colon>0&&colon<closure) {
					if(nextOpener>0&&nextOpener<colon) {
						throw new IllegalArgumentException("Misplaced ${ at "+opener);
					}
					String prop=k.substring(opener+2,colon);
					String val=builder==null?null:resolve(prop);
					if(val!=null) {
						builder.append(val);
					}
					int cCount=0;
					if(colon<k.length()-3&&k.charAt(colon+1)=='['&&k.charAt(colon+3)==']') try {
						cCount=Integer.parseInt(Character.toString(k.charAt(colon+2)), 10);
						colon+=3;
					} catch(Throwable t) {
					}
					offset=map(k,colon+1,nextOpener,cCount,val!=null?null:builder);
					nextOpener=k.indexOf("${",offset);
				} else {
					if(nextOpener>0&&nextOpener<closure) {
						throw new IllegalArgumentException("Misplaced ${ at "+opener);
					}
					String prop=k.substring(opener+2,closure);
					if(builder!=null) {
						String val=resolve(prop);
						if(val!=null) {
							builder.append(val);
						} else {
							// Unknown....
							throw new IllegalArgumentException("Unknown property "+prop+" at position "+(opener+2));
						}
					}
					offset=closure+1;
					
				}
			}
		}
	}

	@Override
	public String map(String k) {
		int nextOpener=k.indexOf("${");
		StringBuilder builder=null;
		if(nextOpener>=0) {
			builder=new StringBuilder();
			map(k,0,nextOpener,k.length()+1,builder);
		}
		return builder==null?k:builder.toString();
	}
}
