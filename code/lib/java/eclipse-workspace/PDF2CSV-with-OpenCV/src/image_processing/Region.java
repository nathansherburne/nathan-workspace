package image_processing;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class Region {
	private Rect bounding;
	private Point centroid;

	public Region(Rect bounding, Point centroid) {
		this.bounding = bounding;
		this.centroid = centroid;
	}

	public Rect getBounding() {
		return bounding;
	}

	public Point getCentroid() {
		return centroid;
	}
}