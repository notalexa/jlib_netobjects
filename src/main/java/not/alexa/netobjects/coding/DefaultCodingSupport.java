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
package not.alexa.netobjects.coding;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.AccessibleObject;

/**
 * Basic coding support for encoders and decoders (for support for text coding schemes see
 * {@link TextCodingSupport}.
 * 
 */
public class DefaultCodingSupport {
    private Map<Object,Integer> objIds;
    private List<AccessibleObject> objRefs;

	public DefaultCodingSupport() {
	}
	    
    /**
     * The method should be called by an encoder.
     *  
     * @param o the object we need a reference for
     * @return the reference for this object if already registered, <code>null</code> if not
     * @throws BaseException never in this implementation since references are supported.
     */
    public int getObjectReference(Object o) throws BaseException {
        if(objIds==null) {
            objIds=new IdentityHashMap<>();
        } else if(objIds.containsKey(o)) {
            return objIds.get(o);
        }
        objIds.put(o,objIds.size());
        return -1;
    }

    /**
     * This method should be called by a decoder to register an object.
     * 
     * @param enableLookup if <code>true</code> register the object internally
     * @param ref if set, add the reference for <code>o</code> as an external reference
     * @param o the accessible object.
     */
    public void addObjectReference(AccessibleObject o) {
    	if(objRefs==null) {
        	objRefs=new ArrayList<AccessibleObject>();
        }
        objRefs.add(o);
    }
    
    /**
     * This method should be called by a decoder to obtain the object associated with the given ref.
     * 
     * @param ref the reference.
     * @return the object assigned to the given ref.
     */
    public AccessibleObject resolveObjectReference(int ref) {
        try {
            return objRefs.get(ref);
        } catch(Throwable t) {
            return null;
        }
    }

}
