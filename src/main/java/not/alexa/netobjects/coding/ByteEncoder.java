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

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.BaseException;

/**
 * Simple wrapper to support byte arrays directly.
 * 
 * @author notalexa
 * @see Encoder
 * @see CodingScheme#createEncoder(Context)
 */
public class ByteEncoder implements Encoder {
	protected ByteArrayOutputStream out;
	protected Encoder encoder;
	protected Charset charset;
	
	/**
	 * This encoder cannot be created directly but is used in {@link CodingScheme#createEncoder(Context)}.
	 *  
	 * @param scheme the underlying coding scheme
	 * @param context the context to use for resolving network types
	 */
	ByteEncoder(CodingScheme scheme,Context context) {
		encoder=scheme.createEncoder(context,out=new ByteArrayOutputStream());
		charset=scheme.getCodingCharset();
	}
	
	/**
	 * Encode the object
	 */
	public ByteEncoder encode(Object o) throws BaseException {
		encoder.encode(o);
		return this;
	}
	
	/**
	 * 
	 * @return the byte array representing the encoded object
	 * 
	 * @throws BaseException if an error occurs
	 */
	public byte[] asBytes() throws BaseException {
		flush();
		return out.toByteArray();
	}
	
	/**
	 * @return the textual form of the encoded object using the charset of {@link CodingScheme#getCodingCharset()} if set, the default
	 * charset otherwise
	 */
	public String toString() {
		if(charset!=null) try {
			return out.toString(charset.name());
		} catch(Throwable t) {
		}
		return out.toString();
	}

	@Override
	public void close() throws BaseException {
		encoder.close();
	}

	@Override
	public void flush() throws BaseException {
		encoder.flush();
	}

    @Override
    public CodingScheme getCodingScheme() {
        return encoder.getCodingScheme();
    }
}
