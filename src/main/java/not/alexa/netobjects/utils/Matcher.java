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
 * A <code>Matcher</code> for type <code>T</code> decides wether an object of the type
 * matches a certain condition.
 * 
 * @author notalexa
 *
 * @param <T> the type of this matcher
 */
public interface Matcher<T> {
    /**
     * 
     * @param t the object to check
     * @return <code>true</code> if t matches the condition
     */
	public boolean matches(T t);
	
	public class Equals<T> implements Matcher<T> {
	    private T t;
	    public Equals(T t) {
	        this.t=t;
	    }
	    
        @Override
        public boolean matches(T t) {
            return this.t==null?t==null:this.t.equals(t);
        }       
	}
	
	public class Not<T> implements Matcher<T> {
		private Matcher<T> matcher;
		public Not(Matcher<T> matcher) {
			this.matcher=matcher;
		}
		
		@Override
		public boolean matches(T t) {
			return !matcher.matches(t);
		}		
	}


	public class And<T> implements Matcher<T> {
		private Matcher<T>[] matchers;
		public And(Matcher<T>...matchers) {
			this.matchers=matchers;
		}
		
		@Override
		public boolean matches(T t) {
			for(Matcher<T> matcher:matchers) {
				if(!matcher.matches(t)) {
					return false;
				}
			}
			return true;
		}		
	}
	
	public class Or<T> implements Matcher<T> {
		private Matcher<T>[] matchers;
		public Or(Matcher<T>...matchers) {
			this.matchers=matchers;
		}
		
		@Override
		public boolean matches(T t) {
			for(Matcher<T> matcher:matchers) {
				if(matcher.matches(t)) {
					return true;
				}
			}
			return false;
		}		
	}
}
