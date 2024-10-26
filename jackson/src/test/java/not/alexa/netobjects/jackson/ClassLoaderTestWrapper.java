/*
 * Copyright (C) 2023 Not Alexa
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
package not.alexa.netobjects.jackson;

import static org.junit.Assert.fail;

import java.io.File;
import java.lang.ref.ReferenceQueue;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import not.alexa.netobjects.utils.BackingClassLoader;

public class ClassLoaderTestWrapper {
	static ReferenceQueue<Class<?>> QUEUE=new ReferenceQueue<>();
	
	@Test
	public void test() {
		try {
			Class.forName("not.alexa.netobjects.jackson.ClassLoaderTest");
			fail("Remove the overlay from the default classpath");
		} catch(ClassNotFoundException e) {
		}
		int count=0;
		List<Object> refList=new ArrayList<>();
		for(int i=0;i<1000;i++) try {
			System.gc();
			count++;
			while(QUEUE.poll()!=null) {
				count--;
			}
			ClassLoader classLoader=new BackingClassLoader(new URL[] { new File("src/test/overlay").getAbsoluteFile().toURI().toURL()}, ClassLoaderTestWrapper.class.getClassLoader());
			Object o=Class.forName("not.alexa.netobjects.jackson.ClassLoaderTest", false, classLoader).newInstance();
			o.getClass().getMethod("run",List.class,ReferenceQueue.class).invoke(o,refList,QUEUE);
		} catch(Throwable t) {
			t.printStackTrace();
		}
		if(count>20) {
			fail("Memory leak in type resolver");
		}
	}

}
