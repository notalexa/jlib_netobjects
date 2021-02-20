package not.alexa.netobjects.utils;

public interface Matcher<T> {
	public boolean matches(T t);
	
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
