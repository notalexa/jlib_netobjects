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
package not.alexa.netobjects.utils;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayUtilsTest {

    public ArrayUtilsTest() {
    }

    @Test public void nullTest() {
        Object[] a=null;
        List<Object> l=null;
        Map<Object,Object> m=null;
        assertNull(ArrayUtils.nullIfEmpty(a));
        assertNull(ArrayUtils.nullIfEmpty(l));
        assertNull(ArrayUtils.nullIfEmpty(m));
        a=new Object[0];
        l=new ArrayList<>();
        m=new HashMap<Object, Object>();
        assertNull(ArrayUtils.nullIfEmpty(a));
        assertNull(ArrayUtils.nullIfEmpty(l));
        assertNull(ArrayUtils.nullIfEmpty(m));
    }
    
    @Test public void nonNullTest() {
        Object[] a=new Object[] {"a"};
        List<Object> l=Collections.singletonList("a");
        Map<Object,Object> m=Collections.singletonMap("a","a");
        assertSame(a,ArrayUtils.nullIfEmpty(a));
        assertSame(l,ArrayUtils.nullIfEmpty(l));
        assertSame(m,ArrayUtils.nullIfEmpty(m));
    }
}
