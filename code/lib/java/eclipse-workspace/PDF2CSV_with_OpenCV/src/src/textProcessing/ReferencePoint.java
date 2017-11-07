package textProcessing;

import java.util.ArrayList;

public abstract class ReferencePoint implements Comparable<ReferencePoint> {
	private ArrayList<MarginPoint> margins = new ArrayList<MarginPoint>();
	private MarginPoint rightMost = null;
	private MarginPoint leftMost = null;

	public ReferencePoint(MarginPoint mp) {
		add(mp);
	}

	public void add(MarginPoint mp) {
		margins.add(mp);

		if (leftMost == null) {
			leftMost = mp;
		} else if (mp.getX() < leftMost.getX()) {
			leftMost = mp;
		}
		if (rightMost == null) {
			rightMost = mp;
		} else if (mp.getX() > rightMost.getX()) {
			rightMost = mp;
		}
	}
	
	public float getLeft() {
		return leftMost.getX();
	}
	
	public float getRight() {
		return rightMost.getX();
	}

	public ArrayList<MarginPoint> getMPs() {
		return margins;
	}
	
	@Override
	public int compareTo(ReferencePoint rp) {
		return (int) (getLeft() - rp.getLeft());
	}

	public abstract boolean isLeft();
	public abstract boolean isRight();

}
