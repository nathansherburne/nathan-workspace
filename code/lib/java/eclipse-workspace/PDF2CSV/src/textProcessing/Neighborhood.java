package textProcessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import technology.tabula.HasText;
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
		this.add(block);
		marginStructure = new MarginStructure();
		marginStructure.addBlock(block);
	}

	public MarginStructure getMarginStructure() {
		return marginStructure;
	}

	public void add(Block block) {
		blocks.add(block);
		marginStructure.addBlock(block);
	}

	public void getReferencePoints(float threshold) {
		List<LeftReferencePoint> LRPs = new ArrayList<LeftReferencePoint>();
		List<RightReferencePoint> RRPs = new ArrayList<RightReferencePoint>();

		MarginPoint prevLeft, curLeft;
		LeftReferencePoint leftRP;
		
		Iterator<MarginPoint> LMPIterator = getMarginStructure().getLeftMPs().values().iterator();
		if (LMPIterator.hasNext()) {
			curLeft = LMPIterator.next();
			leftRP = new LeftReferencePoint(curLeft);
			while (LMPIterator.hasNext()) {
				prevLeft = curLeft;
				curLeft = LMPIterator.next();
				if (curLeft.getX() - prevLeft.getX() <= threshold) {
					leftRP.add(curLeft);
				} else {
					LRPs.add(leftRP);
					leftRP = new LeftReferencePoint(curLeft);
				}
			}
			LRPs.add(leftRP);
		}
		
		MarginPoint prevRight, curRight;
		RightReferencePoint rightRP;
		
		Iterator<MarginPoint> RMPIterator = getMarginStructure().getRightMPs().values().iterator();
		if (RMPIterator.hasNext()) {
			curRight = RMPIterator.next();
			rightRP = new RightReferencePoint(curRight);
			while (RMPIterator.hasNext()) {
				prevRight = curRight;
				curRight = RMPIterator.next();
				if (curRight.getX() - prevRight.getX() <= threshold) {
					rightRP.add(curRight);
				} else {
					RRPs.add(rightRP);
					rightRP = new RightReferencePoint(curRight);
				}
			}
			RRPs.add(rightRP);
		}

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
