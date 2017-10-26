package textProcessing;

import java.util.ArrayList;

public abstract class ReferencePoint implements Comparable<ReferencePoint> {
	private int ref_counter = 0;
	private ArrayList<MarginPoint> margins = new ArrayList<MarginPoint>();
	private MarginPoint rightMost = null;
	private MarginPoint leftMost = null;

	public ReferencePoint(MarginPoint mp) {
		add(mp);
	}

	public void add(MarginPoint mp) {
		margins.add(mp);
		ref_counter += mp.getHeight();

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

	public int getHeight() {
		return ref_counter;
	}
	
	@Override
	public int compareTo(ReferencePoint rp) {
		return (int) (getLeft() - rp.getLeft());
	}

	public abstract boolean isLeft();
	public abstract boolean isRight();

}
