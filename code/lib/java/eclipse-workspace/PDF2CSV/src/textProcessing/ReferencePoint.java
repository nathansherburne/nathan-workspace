package textProcessing;

import java.util.Map;
import java.util.TreeMap;

public class ReferencePoint {
	private int ref_counter = 0;
	private Map<java.lang.Float, MarginPoint> margins = new TreeMap<java.lang.Float, MarginPoint>();
	
	public ReferencePoint() {
		
	}
	
	public void add(MarginPoint mp) {
		margins.put(mp.getX(), mp);
		ref_counter += mp.getReferenceCount();
	}
	
	
}
