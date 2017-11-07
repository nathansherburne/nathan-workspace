package textProcessing;

public abstract class MarginPoint implements Comparable<MarginPoint> {
	private float x;
	
	public MarginPoint(float x, int height) {
		this.x = x;
	}
	// Do margins need reference counters or should I leave that to ReferencePoints?
	// As specified, margins do accumulate blocks with EQUAL left or right x.	
	
	@Override
	public int compareTo(MarginPoint o) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;

	    if (this == o) return EQUAL;
	    if (Float.compare(getX(), o.getX()) == 0) {
	    		if(isLeft() && o.isRight()) {
	    			return AFTER;
	    		} else if (isRight() && o.isLeft()) {
	    			return BEFORE;
	    		} else {
	    			return EQUAL;
	    		}
	    }
	    return Float.compare(getX(), o.getX());
	}
	
	@Override
	public boolean equals(Object o) {
	     if (this == o) return true;
	     if (!(o instanceof MarginPoint)) return false;

	     MarginPoint other = (MarginPoint)o;
	     return isLeft() == other.isLeft() && Float.compare(getX(), other.getX()) == 0;
	}
	
	@Override
	public int hashCode() {
		final int prime = 37;
		int result = 1;
		result = result + prime * (isLeft() ? 0 : 1);
		result = result + prime * Float.floatToIntBits(getX());
		return result;
	}
	
	public float getX() {
		return x;
	}
	
	public abstract boolean isLeft();
	public abstract boolean isRight();
}
