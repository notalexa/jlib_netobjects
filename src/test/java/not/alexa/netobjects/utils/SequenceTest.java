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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import not.alexa.netobjects.BaseException;

public class SequenceTest {

    public SequenceTest() {
    }
    
    @Test public void emptySequenceTest() {
        try(Sequence<Void> empty=Sequence.emptySequence()) {
            assertFalse(empty.remove());
            assertFalse(empty.busy());
            assertNull(empty.current());
            assertSame(empty,empty.next());
        } catch(BaseException e) {
            fail();
        }
     }
    @Test public void iteratorSequenceTest() {
        for(String s:Sequence.<String>emptySequence()) {
            fail();
        }
     }
    
    @Test
    public void fromIteratorSequenceTest() {
        String[] t=new String[] {"a","b"};
        try(Sequence<String> seq=Sequence.from(Arrays.asList(t))) {
            int i=0;
            for(String s:seq) {
                assertEquals(t[i],s);
                i++;
            }
            assertTrue(t.length==i);
        } catch(BaseException e) {
            fail();
        }
    }
    @Test
    public void fromEnumerationSequenceTest() {
        String[] t=new String[] {"a","b"};
        try(Sequence<String> seq=Sequence.from(Collections.enumeration(Arrays.asList(t)))) {
            int i=0;
            for(String s:seq) {
                assertEquals(t[i],s);
                i++;
            }
            assertTrue(t.length==i);
        } catch(BaseException e) {
            fail();
        }
    }
    @Test
    public void removeIteratorSequenceTest() {
        String[] t=new String[] {"a","b"};
        List<String> l=new ArrayList<>(Arrays.asList(t));
        try(Sequence<String> seq=Sequence.from(l)) {
            int i=0;
            for(Cursor<String> c=seq;c.busy();c=c.next()) {
                assertEquals(t[i],c.current());
                assertTrue(c.remove());
                i++;
            }
            assertTrue(t.length==i);
            assertTrue(0==l.size());
        } catch(BaseException e) {
            fail();
        }
    }
    
    @Test
    public void removeIteratorSequenceTest2() {
        String[] t=new String[] {"a","b"};
        List<String> l=new ArrayList<>(Arrays.asList(t));
        try(Sequence<String> seq=Sequence.from(l)) {
            int i=0;
            for(Iterator<String> itr=seq.iterator();itr.hasNext();) {
                assertTrue(itr.hasNext());
                String s=itr.next();
                assertEquals(t[i],s);
                itr.remove();
                i++;
            }
            assertTrue(t.length==i);
            assertTrue(0==l.size());
        } catch(BaseException e) {
            fail();
        }
    }
    @Test
    public void removeIteratorSequenceTest3() {
        String[] t=new String[] {"a"};
        try(Sequence<String> seq=Sequence.from(Arrays.asList(t))) {
            int i=0;
            for(Iterator<String> itr=seq.iterator();itr.hasNext();) {
                assertTrue(itr.hasNext());
                String s=itr.next();
                assertEquals(t[i],s);
                itr.remove();
                i++;
            }
            fail();
        } catch(UnsupportedOperationException e) {
        } catch(BaseException e) {
            fail();
        }
    }
}
