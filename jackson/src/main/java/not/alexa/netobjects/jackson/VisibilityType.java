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
package not.alexa.netobjects.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

/**
 * Enumeration representing the different types of visibility.
 * 
 * @author notalexa
 */
enum VisibilityType {
	Field, Getter, IsGetter, Setter, Creator;
	
	public Visibility getVisibility(Class<?> clazz ) {
		JsonAutoDetect autoDetect=clazz.getAnnotation(JsonAutoDetect.class);
		if(autoDetect!=null) {
			//annotationSeen|=clazz.equals(topClass);
			Visibility v=Visibility.DEFAULT;
			switch(this) {
				case Field: v=autoDetect.fieldVisibility();
					break;
				case Getter: v=autoDetect.getterVisibility();
					break;
				case IsGetter: v=autoDetect.isGetterVisibility();
					break;
				case Setter: v=autoDetect.setterVisibility();
					break;
				case Creator: v=autoDetect.creatorVisibility();
					break;
			}
			if(v!=Visibility.DEFAULT) {
				return v;
			}
		}
		if(clazz.getSuperclass()!=null) {
			return getVisibility(clazz.getSuperclass());
		}
		return Visibility.PUBLIC_ONLY;
	}
	
	public String toName(String n) {
		switch(this) {
			case Setter:
			case Getter: if(n.length()>3) {
					return Character.toLowerCase(n.charAt(3))+n.substring(4);
				}
				break;
			case IsGetter:if(n.length()>2) {
					return Character.toLowerCase(n.charAt(2))+n.substring(3);
				}
				break;
			default:
				break;
		}
		return n;
	}
}
