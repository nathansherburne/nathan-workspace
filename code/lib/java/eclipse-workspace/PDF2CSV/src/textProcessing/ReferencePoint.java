package textProcessing;

import java.util.Map;
import java.util.TreeMap;

public abstract class ReferencePoint {
	private int ref_counter = 0;
	private Map<java.lang.Float, MarginPoint> margins = new TreeMap<java.lang.Float, MarginPoint>();
	
	public ReferencePoint(MarginPoint mp) {
		add(mp);
	}
	
	public void add(MarginPoint mp) {
		margins.put(mp.getX(), mp);
		ref_counter += mp.getReferenceCount();
	}
	
	public abstract boolean isLeft();
	public abstract boolean isRight();

}
