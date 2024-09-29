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
package not.alexa.netobjects.utils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.function.Function;

/**
 * 
 * @param <K>
 * @param <V>
 */
public class WeakReferenceKeyMap<K extends StrongRefPool,V> {
	private WeakKeyMap<K,Reference<V>> map=new WeakKeyMap<K,Reference<V>>();

	public WeakReferenceKeyMap() {
	}

    /**
     * Retrieve a value.
     * 
     * @param key the key
     * @return the resource for the given key or <code>null</code> if no resource is available
     */
    public synchronized V get(K key) {
    	Reference<V> v=map.get(key);
    	return v==null?null:v.get();
    }
    
    /**
     * Store a key,value pair.
     * 
     * @param key the key
     * @param resource the resource for the key
     */
    public synchronized void put(K key,V resource) {
        map.put(key,key.register(new WeakReference<>(resource)));
    }
    
    public synchronized V computeIfAbsent(K key,Function<K,V> creator) {
    	V v=get(key);
    	if(v==null) {
    		v=creator.apply(key);
    		put(key,v);
    	}
    	return v;
    }
    
    /**
     * Remove a key.
     * 
     * @param key the key
     */
    public synchronized void remove(K key) {
        map.remove(key);
    }
    
    /**
     * Get the size of this map.
     * 
     * @return the size of the map (after update)
     */
    public synchronized int size() {
        return map.size();
    }
}
