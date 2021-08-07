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
import not.alexa.netobjects.types.ClassTypeDefinition.Field;

/**
 * Interface representing a (low level) codec for specific types. All primitive types needs a specific codec for a given
 * coding scheme.
 * 
 * @author notalexa
 *
 */
public interface Codec {
    
    /**
     * Encode the given object. The codec can assume that type checking is done.
     * 
     * @param buffer the encoding buffer
     * @param t the object to encode
     * @throws BaseException if an error occurs
     */
	public void encode(Encoder.Buffer buffer,Object t) throws BaseException;
	
	/**
	 * Decode the content of the buffer
	 * @param buffer the decoding buffer
	 * @return the decoded object
	 * @throws BaseException if an error occurs
	 */
	public Object decode(Decoder.Buffer buffer) throws BaseException;
	
	public default Codec getCodec(Field f) throws BaseException {
	    throw new BaseException(BaseException.BAD_REQUEST, "Not a class codec.");	    
	}
	
	public default Codec getComponentCodec() throws BaseException {
       throw new BaseException(BaseException.BAD_REQUEST, "Not an array codec.");
	}
}
