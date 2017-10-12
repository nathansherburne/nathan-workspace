package textProcessing;

import java.util.TreeMap;

public class MarginStructure {
	TreeMap<Float, MarginPoint> rightMarginPoints = new TreeMap<Float, MarginPoint>();
	TreeMap<Float, MarginPoint> leftMarginPoints = new TreeMap<Float, MarginPoint>();
	private float marginRange;
	
	public MarginStructure(float marginRange) {
		this.marginRange = marginRange;
	}
	
	public TreeMap<Float, MarginPoint> getRightMPs() {
		return rightMarginPoints;
	}
	
	public TreeMap<Float, MarginPoint> getLeftMPs() {
		return leftMarginPoints;
	}
	
	public void addBlock(Block block) {
		MarginPoint lmp = addMarginPoint(block.getLeft(), new LeftMarginPoint(block));
		if(lmp != null) {
			lmp.addRefBlock(block);
		}
		MarginPoint rmp = addMarginPoint(block.getRight(), new RightMarginPoint(block));
		if(rmp != null) {
			rmp.addRefBlock(block);
		}
	}
	
	private MarginPoint addMarginPoint(Float x, MarginPoint mp) {
		if(mp.isLeft()) {
			if(leftMarginPoints.containsKey(x)) {
				return leftMarginPoints.get(x);
			}
			else {
				leftMarginPoints.put(x, mp);
			}
		} else if(mp.isRight()) {
			if(rightMarginPoints.containsKey(x)) {
				return rightMarginPoints.get(x);
			}
			else {
				rightMarginPoints.put(x, mp);
			}
		}
		return null;
	}
}
