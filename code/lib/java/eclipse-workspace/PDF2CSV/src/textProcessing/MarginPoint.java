package textProcessing;

import java.util.ArrayList;
import java.util.List;

public abstract class MarginPoint {
	
	// Do margins need reference counters or should I leave that to ReferencePoints?
	// As specified, margins do accumulate blocks with EQUAL left or right x.
	protected List<Block> blocksWithThisMP = new ArrayList<Block>();
	
	public MarginPoint(Block block) {
		blocksWithThisMP.add(block);
	}
	
	public int getReferenceCount() {
		return blocksWithThisMP.size();
	}
	
	public List<Block> getRefBlocks() {
		return blocksWithThisMP;
	}
	
	public void addRefBlock(Block block) {
		blocksWithThisMP.add(block);
	}
	
	public abstract float getX();
	public abstract boolean isLeft();
	public abstract boolean isRight();
}
