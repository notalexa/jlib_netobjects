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
package not.alexa.netobjects.utils;

import java.util.Collection;
import java.util.Map;

/**
 * Static methods for arrays (and collections).
 * 
 * @author notalexa
 *
 */
public class ArrayUtils {

    private ArrayUtils() {
    }

    /**
     * 
     * @param m the map to check
     * @return <code>null</code> if <code>null</code> or empty
     */
    public static Object nullIfEmpty(Map<?,?> m) {
        return m==null||m.size()==0?null:m;
    }
    
    /**
     * 
     * @param c the collection to check
     * @return <code>null</code> if <code>null</code> or empty
     */
    public static Object nullIfEmpty(Collection<?> c) {
        return c==null||c.size()==0?null:c;
    }
    
    /**
     * 
     * @param a the array to check
     * @return <code>null</code> if <code>null</code> or empty
     */
    public static <T> Object nullIfEmpty(T[] a) {
        return a==null||a.length==0?null:a;
    }
}
