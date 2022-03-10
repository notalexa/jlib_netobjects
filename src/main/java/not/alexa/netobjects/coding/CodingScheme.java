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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;

/**
 * Interface representing a coding scheme. A coding scheme is defined by
 * two components, an encoder for encoding objects into "something" and a decoder for decoding
 * "something" into an object. This interface assumes that "something" is
 * represented over the network which implies a form representable as a
 * byte array (or stream).
 * 
 * @author notalexa
 *
 */
public interface CodingScheme {
    
    /**
     * 
     * @return the global systems coding scheme
     */
    public static CodingScheme getSystemScheme() {
        return XMLCodingScheme.DEFAULT_SCHEME;
    }
    
	// May return null if the scheme is a binary scheme
    /**
     * If this scheme assumes that the object is represented in textual form
     * with a fixed or default character set, this method should return the required
     * character set. For objects represented
     * in binary form, this method may return <code>null</code>
     * 
     * @return the (default) character set of this coding scheme
     */
	public default Charset getCodingCharset() {
		return null;
	}
	
	/**
	 * Create an encoder for the given stream. Note that this method
	 * assumes a context for resolving network types.
	 * 
	 * @param context the context to use for resolving network types
	 * @param stream the output stream to encode objects into
	 * @return an encoder for encoding
	 */
	public Encoder createEncoder(Context context,OutputStream stream);
	
	/**
	 * Convenience method to create an encoder encoding into a byte array.
	 * The encoded bytes can be retrieved via {@link ByteEncoder#asBytes()}
	 * or {@link ByteEncoder#toString()} if the output is of type text.
	 * 
	 * @param context the context to use for resolving network types
	 * @return a byte encoder for encoding
	 */
	public default ByteEncoder createEncoder(Context context) {
		return new ByteEncoder(this,context);
	}
	
	/**
	 * Create a decoder for the given input stream.
	 * 
	 * @param context the context to use for resolving network types
	 * @param stream the input stream
	 * @return a decoder for decoding the stream into (network) objects
	 */
	public Decoder createDecoder(Context context,InputStream stream);
	
	/**
	 * Convenience method using directly a byte array instead of the input stream.
	 * 
	 * @param context the context to use for resolving network types
	 * @param bytes the bytes of the encoded object
	 * @return a decoder for decoding the bytes
	 */
	public default Decoder createDecoder(Context context,byte[] bytes) {
		return createDecoder(context,new ByteArrayInputStream(bytes));
	}
}
