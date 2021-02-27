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

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.utils.Sequence;

public interface Decoder extends AutoCloseable {
    /**
     * Close this decoder freeing underlying resources.
     */
	public void close() throws BaseException;
	
	/**
	 * Decode into an object of the given type.
	 * If the underlying stream is not empty and represents a network object not linkable to the underlying class, the action of
	 * this method is undefined. Codingschemes may be configurable to throw a suitable exception in this case or return <code>null</code>.
	 * 
	 * @param <T> the requested type
	 * @param clazz the class representing the type
	 * @return a decoded object of the given type
	 * @throws BaseException if an error occurs
	 */
	public <T> T decode(Class<T> clazz) throws BaseException;
	
	/**
	 * Return a sequence of all objects in the underlying stream. If an error occurs while decoding, the stream is not evaluated any more and
	 * the exception is thrown in the {@link Sequence#close()} method.
	 * 
	 * @param <T> the requested type
	 * @param clazz the class representing the type
	 * @return a sequence representing all objects in the underlying stream
	 */
	public default <T> Sequence<T> decodeAll(Class<T> clazz) {
	    return new Sequence<T>() {
	        boolean read=true;
	        T t;
	        Throwable e;
            @Override
            public boolean busy() {
                if(read) try {
                    read=false;
                    while(e==null&&t!=null) {
                        t=decode(clazz);
                    }
                } catch(Throwable t) {
                    e=t;
                }
                return t!=null;
            }

            @Override
            public T current() {
                return t;
            }

            @Override
            public Sequence<T> next() {
                if(busy()) {
                    read=true;
                }
                return this;
            }

            @Override
            public void close() throws BaseException {
                Decoder.this.close();
                if(e!=null) {
                    BaseException.throwException(e);
                }
            }
	    };
	}

   /**
     * Buffer passed to a specific {@link Codec}
     * 
     * @author notalexa
     *
     */
	public interface Buffer extends AccessContext {
	    /**
	     * Codecs expecting the content in binary form can retrieve the content with this method
	     * @return the content of the field in binary form
	     */
		public byte[] getByteContent();
		
        /**
         * Codecs expecting the content in text form can retrieve the content with this method
         * @return the content of the field in text form
         */
		public CharSequence getCharContent();
	}
}
