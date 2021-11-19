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
import java.util.WeakHashMap;

/**
 * A weak key map stores values for a given key. The map doesn't prevent the key from
 * being garbage collected. In contrast to the <code>WeakHashMap</code> class two keys
 * are considered as equal if they represent the same object. If a key is garbage collected,
 * the entry is removed from the table.s
 *
 * @author notalexa
 *
 * @param <K> the key type
 * @param <V> the value type
 * @see WeakHashMap
 */
public class WeakKeyMap<K, V> {
    private Map<Key<K>, V> map=new HashMap<Key<K>, V>();
    private ReferenceQueue<K> outdated=new ReferenceQueue<K>();

    public WeakKeyMap() {
    }
    
    /**
     * Retrieve a value.
     * 
     * @param key the key
     * @return the resource for the given key or <code>null</code> if no resource is available
     */
    public synchronized V get(K key) {
        update();
        return map.get(new Key<>(key));
    }
    
    /**
     * Store a key,value pair.
     * 
     * @param key the key
     * @param resource the resource for the key
     */
    public synchronized void put(K key,V resource) {
        map.put(new Key<K>(key,outdated),resource);
    }
    
    /**
     * Remove a key.
     * 
     * @param key the key
     */
    public synchronized void remove(K key) {
        map.remove(new Key<>(key));
    }

    private void update() {
        Object o;
        while((o=outdated.poll())!=null) {
            map.remove(o);
        }        
    }
    
    /**
     * Get the size of this map.
     * 
     * @return the size of the map (after update)
     */
    public synchronized int size() {
        update();
        return map.size();
    }
    
    private static class Key<K> extends WeakReference<K> {
        private int h;
        private Key(K referent) {
            super(referent);
            h=referent.hashCode();
        }
        private Key(K referent, ReferenceQueue<? super K> q) {
            super(referent, q);
            h=referent.hashCode();
        }
        
        @Override
        public int hashCode() {
            return h;
        }
        
        @Override
        public boolean equals(Object o) {
            if(o instanceof Key) {
                Key<?> k=(Key<?>)o;
                return k.get()==get();
            }
            return false;
        }
    }

    public void putAll(WeakKeyMap<K,V> codecs) {
        for(Map.Entry<Key<K>, V> entry:codecs.map.entrySet()) {
            K k=entry.getKey().get();
            if(k!=null) {
                put(k,entry.getValue());
            }
        }
    }
}
