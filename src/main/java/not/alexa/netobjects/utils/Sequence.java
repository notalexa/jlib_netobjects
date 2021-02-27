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
 * linked list and do not require object creation in a lot of cases reducing intermediate creation of interfaces. As a second advantage,
 * sequences respect the observation that resources may be closed after iteration over the sequence. Since it extends <code>AutoCloseable</code>
 * sequences can be used in <code>try</code> - <code>catch</code> blocks.
 * <br>As an optional operation, <code>Sequence</code> implementation may implement the {@link #remove()} method removing the current element
 * out of the sequence and indicating this by returning <code>true</code>. At the moment of deletion, the value of {@link #current()} is undefined.
 * The time of updating the underlying structures is undefined too. The implementation can update the structure in the {@link #close()} method
 * realizing more efficient execution.
 * 
 * @author notalexa
 *
 * @param <T>
 */
public interface Sequence<T> extends AutoCloseable,Iterable<T> {
	
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
	 * {@link #current()} may return <code>null</code> as a valid value. This method tells the sequence consumer if iteration
	 * can be stopped.
	 * 
	 * @return <code>true</code> if the sequence is not totally consumed, <code>false</code> otherwise. After returning <code>false</code>
	 * the sequence never returns <code>true</code> any more.
	 */
	public boolean busy();
	
	/**
	 * 
	 * @return the current value of this sequence. This method can be called more than once.
	 */
	public T current();
	
	/**
	 * 
	 * @return the sequence with the next current element.
	 */
	public Sequence<T> next();
	
	/**
	 * Remove is an optional operation. The return value indicates if the operation can be performed in this sequence. Removing the element
	 * can be performed in the {@link #close()} method generating a bulk operation.
	 * 
	 * @return <code>true</code> if the method is implemented and the value is removed. 
	 */
	public default boolean remove() {
		return false;
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
		
		private Sequence<T> seq;
		public SequenceIterator(Sequence<T> seq) {
			this.seq=seq;
		}
		
		@Override
		public boolean hasNext() {
			return seq.busy();
		}
		
		@Override
		public T next() {
			T t=seq.current();
			seq=seq.next();
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
	public class Itr<T> implements Sequence<T> {		
		Iterator<T> itr;
		T current;
		public Itr(Iterator<T> itr) {
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
	public class Enum<T> implements Sequence<T> {
		Enumeration<T> e;
		T current;
		public Enum(Enumeration<T> e) {
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
