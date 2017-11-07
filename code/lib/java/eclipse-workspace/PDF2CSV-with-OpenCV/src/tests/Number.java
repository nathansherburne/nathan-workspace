package tests;

public class Number implements Comparable<Number> {
		int x;
		Number(int x) {
			this.x = x;
		}
		
		public int getX() {
			return x;
		}
		
		@Override
		public String toString() {
			return String.valueOf(x);
		}
		
		@Override
		public int compareTo(Number n) {
			return x - n.getX();
		}
		
	}