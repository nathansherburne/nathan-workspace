package textProcessing;

import java.util.ArrayList;
import java.util.List;

import technology.tabula.HasText;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;

@SuppressWarnings("serial")
public class Neighborhood extends RectangularTextContainer<Block> implements HasText {
	private List<Block> blocks = new ArrayList<Block>();
	private MarginStructure marginStructure;

	public Neighborhood(float top, float left, float width, float height) {
		super(top, left, width, height);
	}

	public Neighborhood(Block block) {
		super(block.y, block.x, block.width, block.height);
		marginStructure = new MarginStructure();
		this.add(block);
	}

	public MarginStructure getMarginStructure() {
		return marginStructure;
	}

	public void add(Block block) {
		blocks.add(block);
		marginStructure.addBlock(block);
	}

	public void mergeIsolated() {
		final int MIN_HEIGHT = 2;
		// get margin points
		// get reference points
		for (ReferencePoint rp : marginStructure.getReferencePoints()) {
			if (rp.getHeight() < MIN_HEIGHT) {
				if (rp.isLeft()) {
					// Potentially merge blocks in this RP with neighbors if in range.
					for (Block b : marginStructure.getBlocks(rp.getMPs())) {
						potentiallyMergeBlockLeft(b);
					}
				}
				if (rp.isRight()) {
					for (Block b : marginStructure.getBlocks(rp.getMPs())) {
						potentiallyMergeBlockRight(b);
					}
				}

			}
		}
		// find reference points L-R within x-range
		// potentially merge their blocks
	}
	
	public void potentiallyMergeBlockLeft(Block b) {
		Block neighbor = null;
		float closest = java.lang.Float.MAX_VALUE;
		for(Block potentialNeighbor : blocks) {
			if(potentialNeighbor.equals(b)) {
				continue;
			}
			if(potentialNeighbor.verticallyOverlaps(b)) {
				float distance = Math.abs(horizontalOverlapValue(b, potentialNeighbor));
				if(distance < closest) {
					closest = distance;
					neighbor = potentialNeighbor;
				}
			}
		}
		if(neighbor == null) return;
		float spacing = horizontalOverlapValue(b, neighbor);
		if(spacing >= mergeThreshold(b)) {
			blocks.remove(b);
			blocks.remove(neighbor);
			blocks.add(neighbor.merge(b));
		}
	}
	
	public void potentiallyMergeBlockRight(Block b) {
		Block neighbor = null;
		float closest = java.lang.Float.MAX_VALUE;
		for(Block potentialNeighbor : blocks) {
			if(b.equals(potentialNeighbor)) {
				continue;
			}
			if(potentialNeighbor.horizontallyOverlaps(b)) {
				float distance = Math.abs(horizontalOverlapValue(b, potentialNeighbor));
				if(distance < closest) {
					closest = distance;
					neighbor = potentialNeighbor;
				}
			}
		}
		if(neighbor == null) return;
		float spacing = horizontalOverlapValue(b, neighbor);
		if(spacing >= mergeThreshold(b)) {
			blocks.remove(b);
			blocks.remove(neighbor);
			blocks.add(neighbor.merge(b));
		}
	}
	
	public float mergeThreshold(Block b) {
		int numberOfSpaces = 3;
		return b.getAvgWidthOfSpace() * numberOfSpaces * -1;
	}
	
	public float horizontalOverlapValue(Rectangle a, Rectangle b) {
		return Math.min(a.getRight(), b.getRight()) - Math.max(a.getLeft(), b.getLeft());
	}

	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getText(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Block> getTextElements() {
		return blocks;
	}

}
