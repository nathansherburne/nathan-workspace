package textProcessing;

public abstract class MarginPoint {
	
	// Do margins need reference counters or should I leave that to ReferencePoints?
	// As specified, margins do accumulate blocks with EQUAL left or right x.
	private int ref_counter = 0;
	protected Block block;
	
	public MarginPoint(Block block) {
		this.block = block;
		addReference();  
	}
	
	public int getReferenceCount() {
		return ref_counter;
	}
	
	public void addReference() {
		ref_counter++;
	}
	
	public abstract float getX();
	public abstract boolean isLeft();
	public abstract boolean isRight();
}
