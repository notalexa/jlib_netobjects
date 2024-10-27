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
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Implementation of a {@link StrongRefPool}. Override the {@link #finalize(Object)} method for action,
 * which should be performed after an object is removed from the pool (that is the strong reference is removed).
 * 
 * @author notalexa
 */
public class StrongRefPoolSupport implements StrongRefPool {
	private Map<Object,PoolItem> references=new IdentityHashMap<Object,PoolItem>();

	public StrongRefPoolSupport() {
	}
	
	@Override
	public synchronized <T extends Reference<?>> T register(T t) {
		Object referent=t.get();
		if(referent!=null) {
			PoolItem item=references.get(referent);
			if(item==null)  {
				references.put(referent, item=new PoolItem(referent));
			}
			item.incr(t);
		}
		return t;
	}
	
	/**
	 * Finalize the object after it is removed from the pool.
	 * 
	 * @param o the object removed from the pool.
	 */
	protected void finalize(Object o) {
		//System.out.println("Finalize "+o);
	}

	/**
	 * Class handling the reference logic.
	 * 
	 * @author notalexa
	 */
	private class PoolItem {
		Object referent;
		ReferenceRef refs;
		private PoolItem(Object referent) {
			this.referent=referent;
		}
		
		void incr(Reference<?> t) {
			refs=new ReferenceRef(t,refs);
		}
		
		class ReferenceRef extends Finalizer.PhantomRef<Reference<?>> implements Runnable {
			private ReferenceRef prev;
			private ReferenceRef next;
			public ReferenceRef(Reference<?> referent,ReferenceRef ref) {
				super(referent);
				if(ref==null) {
					next=prev=this;
				} else {
					next=ref;
					prev=ref.prev;
					ref.prev=this;
					prev.next=this;
				}
			}
			
			public void run() {
				synchronized(PoolItem.this) {
					if(next==this) {
						StrongRefPoolSupport.this.finalize(referent);
						StrongRefPoolSupport.this.references.remove(referent);
					} else {
						prev.next=next;
						next.prev=prev;
						refs=next;
					}
				}
			}			
		}
	}
}
