/*
 * Copyright (C) 2020 Not Alexa
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

import java.util.Enumeration;
import java.util.Iterator;

import not.alexa.netobjects.BaseException;

/**
 * Sequence is an alternative to the <code>Iterable</code> interface. In contrast to this interface, sequences can be implemented as
 * linked list and do not require object creation. In a lot of cases this approach reduces intermediate creation of objects. As a second advantage,
 * sequences respect the observation that resources may be closed after iteration over the sequence. Since it extends <code>AutoCloseable</code>
 * sequences can be used in <code>try</code> - <code>catch</code> blocks.
 * <br>The time of updating the underlying structures is the case of a remove operation is undefined. The implementation can update the structure in the {@link #close()} method
 * realizing more efficient execution.
 * 
 * @author notalexa
 *
 * @param <T> the type of the sequence
 */
public interface Sequence<T> extends AutoCloseable,Iterable<T>, Cursor<T> {
	
	/**
	 * Returns an empty sequence.
	 * @param <T> the type of the sequence.
	 * @return an empty sequence of the given type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Sequence<T> emptySequence() {
		return SequenceIterator.EMPTY;
	}

	/**
	 * 
	 * @param <T> the type of the sequence
	 * @param i the iterable object
	 * @return a sequence based on the iterable object
	 */
	public static <T> Sequence<T> from(Iterable<T> i) {
	    return from(i.iterator());
	}

	/**
	 * 
     * @param <T> the type of the sequence
	 * @param i the iterator
	 * @return a sequence based on the iterator
	 */
	@SuppressWarnings("resource")
    public static <T> Sequence<T> from(Iterator<T> i) {
	    return new Itr<>(i).next();
	}

	/**
	 * 
     * @param <T> the type of the sequence
	 * @param e the enumeration
	 * @return a sequence based on the given enumeration
	 */
	@SuppressWarnings("resource")
    public static <T> Sequence<T> from(Enumeration<T> e) {
	    return new Enum<>(e).next();
	}
	
	/**
	 * Implementing the <code>AutoCloseable</code>. The method should close resources opened for this sequence and may update
	 * underlying structures like arrays on deletion.
	 * 
	 */
	@Override
	public default void close() throws BaseException {
	}
	
	/**
	 * Use this sequence as an iterator. A typical usage is
	 * <pre>
	 * try(Sequence<?> seq:getSequence()) {
	 *   for(Object o:seq) {
	 *   }
	 * }
	 * </pre>
	 *
	 */
	@Override
	default Iterator<T> iterator() {
		return new SequenceIterator<T>(this);
	}
	
	/**
	 * In general not used directly, the class defines an iterator for an underlying sequence.
	 * 
	 * @author notalexa
	 *
	 * @param <T> the type of this iterator
	 */
	public class SequenceIterator<T> implements Iterator<T> {
		@SuppressWarnings("rawtypes")
		private static final Sequence EMPTY=new Sequence() {
			@Override
			public boolean busy() {
				return false;
			}

			@Override
			public Object current() {
				return null;
			}

			@Override
			public Sequence next() {
				return this;
			}
		};
		
		private Cursor<T> seq;
		private boolean move;
		private SequenceIterator(Sequence<T> seq) {
			this.seq=seq;
		}
		
		@Override
		public boolean hasNext() {
		    if(move) {
		        seq=seq.next();
		        move=false;
		    }
			return seq.busy();
		}
		
		@Override
		public T next() {
			T t=seq.current();
			move=true;
			return t;
		}

		@Override
		public void remove() {
			if(!seq.remove()) {
				Iterator.super.remove();
			}
		}
	}
	
    /**
   * A sequence based on an iterator.
   * 
   * @author notalexa
   *
   * @param <T> the type of the sequence
   */
   final class Itr<T> implements Sequence<T> {        
      Iterator<T> itr;
      T current;
      private Itr(Iterator<T> itr) {
          this.itr=itr;
      }
      
      @Override
      public T current() {
          return current;
      }
      
      @Override
      public Sequence<T> next() {
          if(itr.hasNext()) {
              current=itr.next();
              return this;
          } else {
              return emptySequence();
          }
      }

      @Override
      public boolean busy() {
          return true;
      }

      @Override
      public boolean remove() {
          try {
              itr.remove();
              return true;
          } catch(Throwable t) {
              return false;
          }
      }
  }

  /**
   * A sequence based on an enumeration.
   * 
   * @author notalexa
   *
   * @param <T> the type of this sequence
   */
  final class Enum<T> implements Sequence<T> {
      Enumeration<T> e;
      T current;
      private Enum(Enumeration<T> e) {
          this.e=e;
      }
      @Override
      public T current() {
          return current;
      }
      @Override
      public Sequence<T> next() {
          if(e.hasMoreElements()) {
              current=e.nextElement();
              return this;
          } else {
              return emptySequence();
          }
      }
      @Override
      public boolean busy() {
          return true;
      }
  }
}
