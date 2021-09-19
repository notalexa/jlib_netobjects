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

/**
 * Definition of a cursor moving over a sequence of values. In most cases, this type is not returned. Use a {@link Sequence} instead to control resource handling.
 * <br>As an optional operation, <code>Sequence</code> implementation may implement the {@link #remove()} method removing the current element
 * out of the sequence and indicating this by returning <code>true</code>. At the moment of deletion, the value of {@link #current()} is undefined.
 * 
 * @author notalexa
 *
 * @param <T> the type of this cursor
 */
public interface Cursor<T> {

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
     * @return the cursor pointing to the next element.
     */
    public Cursor<T> next();
    
    /**
     * Remove is an optional operation. The return value indicates if the operation can be performed in this sequence. Removing the element
     * can be performed in the {@link #close()} method generating a bulk operation.
     * 
     * @return <code>true</code> if the method is implemented and the value is removed. 
     */
    public default boolean remove() {
        return false;
    }
}
