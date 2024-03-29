/*
 * Copyright (C) 2022 Not Alexa
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
package not.alexa.netobjects;

/**
 * The interface defines a main method. This is not declared as static and
 * expects a context in which the method should be executed.
 * 
 * @author notalexa
 *
 */
public interface Executable {
    /**
     * The main method of the executable
     * @param context the context in which the main method should be evaluated
     * @throws BaseException if an error occurs
     */
    public void main(Context context) throws BaseException;
}
