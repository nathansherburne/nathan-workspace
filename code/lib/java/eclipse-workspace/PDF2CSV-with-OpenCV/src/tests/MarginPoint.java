package tests;

public abstract class MarginPoint implements Comparable<MarginPoint> {
	Float x;
	
	public MarginPoint(Float x) {
		this.x = x;
	}
	
	public void update(Float x) {
		this.x = x;
	}
	
	public float getX() {
		return x;
	}
	
	@Override
	public int compareTo(MarginPoint m) {
		return x.compareTo(m.getX());
	}
	
	public abstract boolean isLeft();
	public abstract boolean isRight();
}
