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
package not.alexa.netobjects.types.access;

import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.utils.Sequence;

/**
 * As mentioned in the introduction of {@link Access}, an accessible object can be constructed out of access and a general object by "contraction".
 * This approach is provided by this class.
 * @author notalexa
 *
 */
public class DefaultAccessibleObject extends AbstractAccessibleObject implements AccessibleObject,Sequence<AccessibleObject> {
	protected Object o;
	
	/**
	 * Extensions of this class may serve as the object itself. In this case, only access is needed.
	 * 
	 * @param access the access for this object
	 */
	public DefaultAccessibleObject(Access access) {
	    super(access);
		o=this;
	}
	
	/**
	 * In the general case, the object itself is exterior to this class and needs to be specified.
	 * 
	 * @param access the access for the object
	 * @param o the object itself
	 */
	public DefaultAccessibleObject(Access access,Object o) {
	    super(access);
		this.o=o;
	}

    @Override
    public Object getObject() {
        return o;
    }
	
}
