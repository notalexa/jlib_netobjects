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
package not.alexa.netobjects.coding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.access.AccessFactory;

/**
 * Basic text coding support. This class ensures
 * <ul>
 * <li>Access to the coding scheme and (coding) context
 * <li>Functionality for {@link ClassTypeDefinition#enableObjectRefs()}. If a type definition
 * requests object references, the encoder can obtain a reference to the object via {@link #getObjectReference(Object)}
 * (the method also define a reference for the object if not already present (and returns <code>null</code> in this case)).
 * The decoder <b>must</b> call {@link #addObjectReference(boolean, String, AccessibleObject)} with <code>enableLookup</code>
 * set to <code>true</code> for any object which has a type definition with {@link ClassTypeDefinition#enableObjectRefs()} set to
 * <code>true</code> and can use {@link #resolveObjectReference(String)} to obtain the object with the given reference.
 * </ul>

 * @author notalexa
 *
 * @param <S>
 */
public class TextCodingSupport<S extends AbstractTextCodingScheme> {
    private S scheme;
    private Map<Object,Object> objIds;
    private List<AccessibleObject> objRefs;
    private Map<String,AccessibleObject> externalRefs;
    private Context context;
    private AccessFactory accessFactory;
    
    public TextCodingSupport(S scheme,Context context) {
        this.context=context;
        this.scheme=scheme;
        this.accessFactory=scheme.getFactory().forContext(context);
    }
    
    public S getCodingScheme() {
        return scheme;
    }
    
    public Context getContext() {
        return context;
    }
    
    public AccessFactory getFactory() {
        return accessFactory;
    }
    
    /**
     * Customizable method for object reference creation.
     * 
     * @param n the index of the object.
     * @param o the object itself
     * @return a reference object to the object <code>o</code>. The default implementation returns <code>n</code>
     * @see #parseRef(String)
     */
    protected Object defineRef(int n,Object o) {
        return n;
    }
    
    /**
     * The method should be called by an encoder.
     *  
     * @param o the object we need a reference for
     * @return the reference for this object if already registered, <code>null</code> if not
     * @throws BaseException never in this implementation since references are supported.
     */
    public Object getObjectReference(Object o) throws BaseException {
        if(objIds==null) {
            objIds=new IdentityHashMap<>();
        } else if(objIds.containsKey(o)) {
            return objIds.get(o);
        }
        objIds.put(o,defineRef(objIds.size(),o));
        return null;
    }


    /**
     * This method must be called by a decoder.
     * 
     * @param enableLookup if <code>true</code> register the object internally
     * @param ref if set, add the reference for <code>o</code> as an external reference
     * @param o the accessible object.
     */
    public void addObjectReference(boolean enableLookup,String ref,AccessibleObject o) {
        if(enableLookup) {
            if(objRefs==null) {
                objRefs=new ArrayList<AccessibleObject>();
            }
            objRefs.add(o);
        }
        if(ref!=null) {
            if(externalRefs==null) {
                externalRefs=new HashMap<String, AccessibleObject>();
            }
            externalRefs.put(ref,o);
        }
    }
    
    /**
     * Customizable method for parsing a reference to an object. This method <b>must be</b> the opposite of {@link #defineRef(int, Object)}.
     * @param ref the reference
     * @return the index for the given reference
     */
    protected int parseRef(String ref) {
        return Integer.parseInt(ref);
    }
    
    /**
     * This method must be called by a decoder to obtain the object associated with the given ref.
     * 
     * @param ref the reference.
     * @return the object assigned to the given ref.
     */
    public AccessibleObject resolveObjectReference(String ref) {
        if(externalRefs!=null) {
            AccessibleObject o=externalRefs.get(ref);
            if(o!=null) {
                return o;
            }
        }
        try {
            return objRefs.get(parseRef(ref));
        } catch(Throwable t) {
            return null;
        }
    }
}
