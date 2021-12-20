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
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;

/**
 * This interface represents an object which is able to encode a given (java) object. Objects of this type are
 * typically not instantiated directly but by calling the corresponding factory method {@link CodingScheme#createEncoder(not.alexa.netobjects.Context, java.io.OutputStream)}
 * for a given output stream.
 * 
 * @author notalexa
 *
 */
public interface Encoder extends AutoCloseable {
	public default void flush() throws BaseException {
	}
	/**
	 * 
	 * @return the coding scheme which created this encoder.
	 */
	public CodingScheme getCodingScheme();
	
	/**
	 * 
	 * @return <code>true</code> if this encoder supports multiple objects.
	 */
	public default boolean supportsMultipleObjects() {
	    return false;
	}
	
	/**
	 * Close this encoder. In general, this will <b>not</b> close the underlying output stream but may free some intermediate resources.
	 * 
	 */
	public void close() throws BaseException;
	
	/**
	 * Encoded the given object.
	 * 
	 * @param o the object to encode
	 * @return this encoder (for calling {@link #flush()} for example).
	 * @throws BaseException if an error occurs. If the encoder doesn't support multiple objects and this method is called
	 * the second time an exception should be thrown with code {@link BaseException#BAD_REQUEST}.
	 */
	public Encoder encode(Object o) throws BaseException;
	

	/**
	 * Buffer passed to a specific {@link Codec}
	 * 
	 * @author notalexa
	 *
	 */
	public interface Buffer {
	    /**
	     * Push a field and return an encoder for the given {@link Field#getType()}.
	     * 
         * @param ctx an additional (and optional) context object. This object is typically implementation dependent and can
         * be used to do some optimization.
	     * @param f the field to push
	     * @return an encoder for the content of the field
         * @throws BaseException if an error occurs (for example, if the underlying coding scheme encoded into a text)
	     */
		public Encoder push(Object ctx,Field f) throws BaseException;
		
		/**
		 * For simple types, specific codecs directly writes the encoded form into the buffer.
		 * This method can be used if the underlying coding scheme encodes into binary data.
		 * 
		 * @param encoded the encoded bytes of the given field
		 * @throws BaseException if an error occurs (for example, if the underlying coding scheme encoded into a text)
		 */
		public void write(byte[] encoded) throws BaseException;
		
        /**
         * For simple types, specific codecs directly writes the encoded form into the buffer.
         * This method can be used if the underlying coding scheme encodes into text.
         * 
         * @param encoded the encoded bytes of the given field
         * @throws BaseException if an error occurs (for example, if the underlying coding scheme encoded into a binary form)
         */
		public void write(CharSequence encoded) throws BaseException;
		
		/**
		 * Method to support {@link ClassTypeDefinition#enableObjectRefs()}. The method returns true if and only if the object is
		 * already referenced in the stream. In this case, the encoder creates the specific encoding for the reference.
		 * 
		 * @param o the object we need to check
		 * @return <code>true</code> if and only if the object is already referenced.
		 * @throws BaseException if an error occurs while checking (for example, if the reference cannot be encoded or if the coding
		 * scheme doesn't support object references but the object requires it.)
		 */
		public default boolean isReferenced(Object o) throws BaseException {
			return false;
		}
	}
}
