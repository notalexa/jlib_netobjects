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
package not.alexa.netobjects.api;

import java.lang.reflect.AnnotatedElement;

/**
 * Helper class to simplify access to annotations which can be repreated.
 * 
 */
public class Helper {
	private static final CodingHint[] NO_HINTS=new CodingHint[0];
	private static final Field[] NO_FIELDS=new Field[0];

	private  Helper() {
	}
	
	/**
	 * @param e the annotated element
	 * @return all {@link CodingHint}s for the element.
	 */
	public static CodingHint[] getCodingHints(AnnotatedElement e) {
    	CodingHints fields=e.getAnnotation(CodingHints.class);
    	if(fields!=null) {
    		return fields.value();
    	}
        not.alexa.netobjects.api.CodingHint f=e.getAnnotation(not.alexa.netobjects.api.CodingHint.class);
        if(f!=null) {
        	return new not.alexa.netobjects.api.CodingHint[] { f };
        }
        return NO_HINTS;
	}
	
	/**
	 * @param e the annotated element
	 * @return all {@link Field}s for the element.
	 */
    public static Field[] getFields(AnnotatedElement e) {
    	Fields fields=e.getAnnotation(Fields.class);
    	if(fields!=null) {
    		return fields.value();
    	}
        not.alexa.netobjects.api.Field f=e.getAnnotation(not.alexa.netobjects.api.Field.class);
        if(f!=null) {
        	return new not.alexa.netobjects.api.Field[] { f };
        }
        return NO_FIELDS;
    }
}
