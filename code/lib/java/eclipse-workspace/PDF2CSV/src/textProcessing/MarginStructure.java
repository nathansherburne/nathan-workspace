package textProcessing;

import java.util.Map;
import java.util.TreeMap;

import technology.tabula.Rectangle;

public class MarginStructure {
	
	private float marginRange;
	private Map<java.lang.Float, MarginPoint> margins = new TreeMap<java.lang.Float, MarginPoint>();
	
	public MarginStructure(float marginRange) {
		this.marginRange = marginRange;
	}
	
	public void addMarginPoints(Rectangle rect) {
		addMarginPoint(new LeftMarginPoint(rect.getLeft()));
		addMarginPoint(new RightMarginPoint(rect.getRight()));
	}
	
	public void addMarginPoint(MarginPoint mp) {
		if (margins.containsKey(mp.getX())) {
			margins.get(mp.getX()).addReference();
		} else {
			margins.put(mp.getX(), mp);
		}
	}
}
