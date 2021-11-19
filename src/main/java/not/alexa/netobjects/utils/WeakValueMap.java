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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * A weak value map stores values for a given key. Storing the value doesn't prevent
 * the object from being garbage collected. In this case the entry is removed from
 * the map if teh value is not longer referenced somewhere in the system.
 *
 * @author notalexa
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class WeakValueMap<K, V> {
    private Map<K,Holder<K,V>> map=new HashMap<>();
    private ReferenceQueue<V> outdated=new ReferenceQueue<V>();

    public WeakValueMap() {
    }
    
    /**
     * Retrieve a value.
     * 
     * @param key the key
     * @return the resource for the given key or <code>null</code> if no resource is available
     */
    public synchronized V get(K key) {
        update();
        Holder<?,V> h=map.get(key);
        return h==null?null:h.get();
    }
    
    /**
     * Store a key, value pair.
     * 
     * @param key the key
     * @param resource the resource for the key
     */
    public synchronized void put(K key,V resource) {
        map.put(key,new Holder<>(key,resource,outdated));
    }
    
    /**
     * Remove a key.
     * 
     * @param key the key
     */
    public synchronized void remove(K key) {
        map.remove(key);
    }

    private void update() {
        Object o;
        while((o=outdated.poll())!=null) {
            Object current=map.get(o);
            if(current==o) {
                map.remove(o);
            }
        }        
    }
    
    /**
     * Get the size of this map. Entries not longer referenced are deleted from the map.
     * 
     * @return the size of the map (after update)
     */
    public synchronized int size() {
        update();
        return map.size();
    }
    
    private static class Holder<K,R> extends WeakReference<R> {
        K key;
        private Holder(K key,R referent, ReferenceQueue<? super R> q) {
            super(referent, q);
            this.key=key;
        }
        
        @Override
        public int hashCode() {
            return key.hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            return o.equals(key);
        }
    }
}
