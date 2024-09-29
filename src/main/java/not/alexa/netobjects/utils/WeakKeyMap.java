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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * A weak key map stores values for a given key. The map doesn't prevent the key from
 * being garbage collected. In contrast to the <code>WeakHashMap</code> class two keys
 * are considered as equal if they represent the same object. If a key is garbage collected,
 * the entry is removed from the table.
 *
 * @author notalexa
 *
 * @param <K> the key type
 * @param <V> the value type
 * @see WeakHashMap
 */
public class WeakKeyMap<K, V> {
    private Map<Key, V> map=new HashMap<Key, V>();

    public WeakKeyMap() {
    }
    
    /**
     * Retrieve a value.
     * 
     * @param key the key
     * @return the resource for the given key or <code>null</code> if no resource is available
     */
    public synchronized V get(K key) {
        return map.get(new Key(key));
    }
    
    /**
     * Store a key,value pair.
     * 
     * @param key the key
     * @param resource the resource for the key
     */
    public synchronized void put(K key,V resource) {
        map.put(new Key(key),resource);
    }

    public void putAll(WeakKeyMap<K,V> other) {
    	// Need to create a new ref.
        for(Map.Entry<Key, V> entry:other.map.entrySet()) {
            K k=entry.getKey().get();
            V v=entry.getValue();
            if(k!=null&&v!=null) {
                put(k,v);
            }
        }
    }
    
    public synchronized V computeIfAbsent(K key,Function<K,V> creator) {
    	V v=get(key);
    	if(v==null) {
    		v=creator.apply(key);
    		if(v!=null) {
    			put(key,v);
    		}
    	}
    	return v;
    }
    
    /**
     * Remove a key.
     * 
     * @param key the key
     */
    public synchronized void remove(K key) {
        map.remove(new Key(key));
    }
    
    /**
     * Get the size of this map.
     * 
     * @return the size of the map (after update)
     */
    public synchronized int size() {
        return map.size();
    }
    
    private class Key extends Finalizer.Ref<K> {
        private int h;
        private Key(K referent) {
            super(referent);
            h=referent.hashCode();
        }
        
        @Override
        public void run() {
        	map.remove(this);
        }
        
        @Override
        public int hashCode() {
            return h;
        }
        
        @Override
        public boolean equals(Object o) {
        	if(o==this) {
        		return true;
        	} else if(o instanceof WeakKeyMap.Key) {
                WeakKeyMap<K,V>.Key k=(WeakKeyMap<K,V>.Key)o;
                return k.get()==get();
            } else {
            	// Breaks reflexivity. But this class is only used locally.
            	return false;
            }
        }
    }
}
