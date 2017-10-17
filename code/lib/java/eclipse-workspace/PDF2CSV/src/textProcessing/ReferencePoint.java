package textProcessing;

import java.util.ArrayList;

public abstract class ReferencePoint {
	private int ref_counter = 0;
	private ArrayList<MarginPoint> margins = new ArrayList<MarginPoint>();
	
	public ReferencePoint(MarginPoint mp) {
		add(mp);
	}
	
	public void add(MarginPoint mp) {
		margins.add(mp);
		ref_counter += mp.getHeight();
	}
	
	public ArrayList<MarginPoint> getMPs() {
		return margins;
	}
	
	public int getHeight() {
		return ref_counter;
	}
	
	public abstract boolean isLeft();
	public abstract boolean isRight();

}
