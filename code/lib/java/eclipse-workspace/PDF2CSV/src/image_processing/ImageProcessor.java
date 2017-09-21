package image_processing;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import utils.MyUtils;

public class ImageProcessor {
	private BufferedImage original;
	private Mat image;
	private Mat edges, bw, horizontal, vertical, mask, closed, blur, hough;
	private List<Rect> boundRects;
	private List<Rect> tables;
	private int line_scale, hole_scale, table_scale;
	private Region[] regions = null;
	private List<Line2D.Float> verticalRulings;
	private List<Line2D.Float> horizontalRulings;
	
	public ImageProcessor(BufferedImage src) throws IOException {
		original = src;
		this.image = initialize(src);
		tables = new ArrayList<Rect>();
		line_scale = 20; // play with this variable in order to increase/decrease the amount of lines to
							// be detected.
		hole_scale = 300; // play with this variable in order to increase/decrease the amount of holes to
							// be detected.
		table_scale = 25; // Must be at least a 15th of the area of the page to be considered.
		verticalRulings = new ArrayList<Line2D.Float>();
		horizontalRulings = new ArrayList<Line2D.Float>();
		process();
	}

	public Mat initialize(BufferedImage buf_image) throws IOException {
		Mat mat_image = img2Mat(buf_image);
		if (mat_image.empty()) {
			System.err.println("Problem loading image.");
			// throw error?
		}
		return mat_image;
	}

	/**
	 * 
	 * @param in
	 * @return a Mat which was converted from in.
	 */
	public Mat img2Mat(BufferedImage in) {
		if (in.getType() != BufferedImage.TYPE_3BYTE_BGR) {
			// in = utils.MyUtils.convertTo3BYTE_BGR(in);
			in = MyUtils.toBufferedImageOfType(in, BufferedImage.TYPE_3BYTE_BGR);
		}
		byte[] pixels = ((DataBufferByte) in.getRaster().getDataBuffer()).getData();
		Mat out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC3);
		out.put(0, 0, pixels);
		return out;
	}

	public List<Rect> getTableRects() {
		return tables;
	}

	public Mat getImage() {
		return image;
	}

	public BufferedImage original() {
		return original;
	}

	public BufferedImage converted() {
		return MyUtils.toBufferedImage(image);
	}

	public BufferedImage horizontal() {
		return MyUtils.toBufferedImage(horizontal);
	}

	public BufferedImage mask() {
		return MyUtils.toBufferedImage(mask);
	}

	public BufferedImage vertical() {
		return MyUtils.toBufferedImage(vertical);
	}

	public BufferedImage bw() {
		return MyUtils.toBufferedImage(bw);
	}

	public BufferedImage closed() {
		return MyUtils.toBufferedImage(closed);
	}

	public BufferedImage blurred() {
		return MyUtils.toBufferedImage(blur);
	}

	public BufferedImage hough() {
		return MyUtils.toBufferedImage(hough);
	}

	/**
	 * Connects line segments in line with one another. A large scale value will
	 * only fill small holes. Scale should be as small as possible insomuch as it
	 * does not allow important elements of the image to be filled in. In detecting
	 * tables, for example, the scale value should not let cells be filled in. The
	 * smaller the scale, the bigger the hole gets filled in.
	 * 
	 * Does not connect gaps in corners.
	 * 
	 * @param bw
	 * @return
	 */
	public Mat close(Mat bw) {
		Mat closed = bw.clone();
		// Close horizontal holes.
		int horizontalsize = closed.cols() / hole_scale;
		Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horizontalsize, 1));
		Imgproc.dilate(closed, closed, horizontalStructure);
		Imgproc.erode(closed, closed, horizontalStructure);

		// Close vertical holes.
		int verticalsize = closed.rows() / hole_scale;
		Mat verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, verticalsize));
		Imgproc.dilate(closed, closed, verticalStructure);
		Imgproc.erode(closed, closed, verticalStructure);
		return closed;
	}

	/**
	 * Uses a process called "opening" to remove non-horizontal lines.
	 * 
	 * @param bw
	 *            a binary Mat (usually that has been processed through adaptive
	 *            thresholding).
	 * @return a binary Mat with only horizontal lines from bw.
	 */
	private Mat getHorizontalLines(Mat bw) {
		Mat horizontal = bw.clone();
		int horizontalsize = horizontal.cols() / line_scale;
		Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horizontalsize, 1));
		Imgproc.erode(horizontal, horizontal, horizontalStructure);
		Imgproc.dilate(horizontal, horizontal, horizontalStructure);
		return horizontal;
	}

	/**
	 * Uses a process called "opening" to remove non-vertical lines.
	 * 
	 * @param bw
	 *            a binary Mat (usually that has been processed through adaptive
	 *            thresholding).
	 * @return a binary Mat with only vertical lines from bw.
	 */
	private Mat getVerticalLines(Mat bw) {
		Mat vertical = bw.clone();
		int verticalsize = vertical.rows() / line_scale;
		Mat verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, verticalsize));
		Imgproc.erode(vertical, vertical, verticalStructure);
		Imgproc.dilate(vertical, vertical, verticalStructure);
		return vertical;
	}

	/**
	 * Find the joints between the lines of the tables, we will use this information
	 * in order to discriminate tables from pictures (tables will contain more than
	 * 4 joints while a picture only 4 (i.e. at the corners))
	 * 
	 * @param horizontal
	 *            a binary Mat of horizontal lines.
	 * @param vertical
	 *            a binary Mat of vertical lines.
	 * @return a binary Mat of the intersections, or joints, between every vertical
	 *         and horizontal line.
	 */
	private Mat getJoints(Mat horizontal, Mat vertical) {
		Mat joints = new Mat();
		Core.bitwise_and(horizontal, vertical, joints);
		return joints;
	}

	/**
	 * Find external contours from the mask, which most probably will belong to
	 * tables or to images
	 * 
	 * @param mask
	 *            a binary Mat to find contours in.
	 * @return only extreme outer contours (i.e. hierarchy-0 level contours).
	 */
	private List<MatOfPoint> getOuterContours(Mat mask) {
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE,
				new Point(0, 0));
		return contours;
	}

	/**
	 * Distinguishes a simple rectangle from potential tables by finding the number
	 * of joints in the region of interest. If the number of joints is 4 or less, it
	 * is probably a rectangle or not a table.
	 * 
	 * @param roi
	 *            the region to determine if a table or not.
	 * @return true if roi is a table, false if not.
	 */
	private boolean isTable(Mat roi) {
		List<MatOfPoint> joints_contours = new ArrayList<MatOfPoint>();
		MatOfInt4 joints_hierarchy = new MatOfInt4();

		Imgproc.findContours(roi, joints_contours, joints_hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		return joints_contours.size() > 4;
	}

	private Rect getBoundRect(MatOfPoint contour) {
		// approxPolyDP takes MatOfPoint2f, so convert.
		MatOfPoint2f contour2f = new MatOfPoint2f();
		contour.convertTo(contour2f, CvType.CV_32FC2);

		MatOfPoint2f approx_contour = new MatOfPoint2f();
		Imgproc.approxPolyDP(contour2f, approx_contour, 3, true);

		// Imgproc.boundingRect takes MatOfPoint, so convert back.
		MatOfPoint contour_poly = new MatOfPoint();
		approx_contour.convertTo(contour_poly, CvType.CV_32S);
		return Imgproc.boundingRect(contour_poly);
	}

	private List<Rect> getRects(Mat bw) {
		List<MatOfPoint> contours = getOuterContours(bw);
		List<Rect> rects = new ArrayList<Rect>();
		for (int i = 0; i < contours.size(); i++) {
			Rect boundRect = getBoundRect(contours.get(i));
			rects.add(boundRect);
		}
		return rects;
	}

	private List<Rect> getTables(List<Rect> boundRects, Mat joints) {
		List<Rect> tables = new ArrayList<Rect>();
		for (Rect boundRect : boundRects) {
			// find the number of joints that each table has
			if (isTable(joints.submat(boundRect))) {
				tables.add(boundRect);
			}
		}
		return tables;
	}

	private Mat toGrayscale(Mat image) {
		Mat gray = new Mat();
		if (image.channels() == 3) {
			Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		} else {
			gray = image;
		}
		return gray;
	}

	private Region[] getRegions(Mat image) {
		Mat labeled = new Mat(image.size(), image.type());

		// Extract components
		Mat rectComponents = Mat.zeros(new Size(0, 0), 0);
		Mat centComponents = Mat.zeros(new Size(0, 0), 0);
		Imgproc.connectedComponentsWithStats(image, labeled, rectComponents, centComponents);

		// Collect regions info
		int[] rectangleInfo = new int[5];
		double[] centroidInfo = new double[2];
		Region[] regions = new Region[rectComponents.rows() - 1];

		for (int i = 1; i < rectComponents.rows(); i++) {

			// Extract bounding box
			rectComponents.row(i).get(0, 0, rectangleInfo);
			Rect rectangle = new Rect(rectangleInfo[0], rectangleInfo[1], rectangleInfo[2], rectangleInfo[3]);

			// Extract centroids
			centComponents.row(i).get(0, 0, centroidInfo);
			Point centroid = new Point(centroidInfo[0], centroidInfo[1]);

			regions[i - 1] = new Region(rectangle, centroid);
		}
		
		return regions;
	}

	public List<Line2D.Float> getLines(Mat disconnectedLines) {
		List<Line2D.Float> lines = new ArrayList<Line2D.Float>();

		// Since the input image is supposed to contain a bunch of disconnected lines,
		// finding "connected components" will actually find the coordinates of the
		// lines, each line being it's own connected component.

		Region[] lineRegions = getRegions(disconnectedLines);
		for(int i = 0; i < lineRegions.length; i++) {
			lines.add(rectToLine(lineRegions[i].getBounding()));
		}
		return lines;
	}
	
	public Line2D.Float rectToLine(Rect r) {
		if(isHorizontal(r)) {
			float avgY = (r.y + (r.y + r.height)) / 2;
			return new Line2D.Float(new Point2D.Float(r.x, avgY), new Point2D.Float(r.x + r.width, avgY));
		}
		float avgX = (r.x + (r.x + r.width)) / 2;
		return new Line2D.Float(new Point2D.Float(avgX, r.y), new Point2D.Float(avgX, r.y + r.height));
	}
	
	public boolean isHorizontal(Rect r) {
		return r.width > r.height;
	}
	
	public List<Line2D.Float> getRulings() {
		return Stream.concat(horizontalRulings.stream(), verticalRulings.stream())
        .collect(Collectors.toList());
	}
	
	private void process() {
		// Some images have white lines on black background. Other images have black
		// lines on white backgrounds. Use Canny edge detector to make lines white no
		// matter what the background and line colors are. The important part is not
		// edge detection, it is making the edges white.
		edges = new Mat();
		bw = new Mat();
		Imgproc.Canny(image, edges, 50, 150, 3, true);

		// Blur to connect corners.
		blur = new Mat();
		Imgproc.GaussianBlur(edges, blur, new Size(11.0, 11.0), 0.0);
		Imgproc.adaptiveThreshold(blur, bw, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 0);

		// Connect colinear line segments.
		closed = close(bw);
		horizontal = getHorizontalLines(closed);
		vertical = getVerticalLines(closed);

		// Connect vertical and horizontal.
		mask = new Mat();
		Core.add(horizontal, vertical, mask);

		// Find connected components.
		regions = getRegions(mask);

		// Get ruling lines.
		horizontalRulings = getLines(horizontal);
		verticalRulings = getLines(vertical);

		// TDOD: get new hor and ver for joints.
		// joints = getJoints(horizontal, vertical);
		// boundRects = getRects(comp);
		// tables = getTables(boundRects, joints);
	}

	/**
	 * 
	 * @param image
	 * @param roiRect
	 * @return a Mat of the original image with all pixels except those in the roi
	 *         set to 0.
	 */
	public Mat getRoi(Mat image, Rect roiRect) {
		Mat mask = new Mat(image.rows(), image.cols(), 0, new Scalar(0));
		Mat roi = mask.submat(roiRect);
		roi.setTo(new Scalar(255));
		Core.bitwise_and(roi, image, roi);
		return roi;
	}

	// Saving for later in case I want to use it.
	public void houghlines() {
		List<Line2D.Float> rulings = new ArrayList<Line2D.Float>();
		Mat lines = new Mat();
		Imgproc.HoughLinesP(mask, lines, 3, Math.PI / 180, 50, 60, 10);
		hough = new Mat(mask.rows(), mask.cols(), CvType.CV_8UC3);
		int colorNum = 0;
		Color color = MyUtils.KELLY_COLORS[colorNum++];
		for (int i = 0; i < lines.rows(); i++) {
			double[] val = lines.get(i, 0);
			rulings.add(new Line2D.Float(new Point2D.Float((float) val[0], (float) val[1]),
					new Point2D.Float((float) val[2], (float) val[3])));
			Point p1 = new Point(val[0], val[1]);
			Point p2 = new Point(val[2], val[3]);
			for (int j = 0; j < regions.length; j++) {
				if (regions[j].getBounding().contains(p1) && regions[j].getBounding().contains(p2)) {
					color = MyUtils.KELLY_COLORS[j];
				}
			}
			Imgproc.line(hough, p1, p2, new Scalar(color.getBlue(), color.getGreen(), color.getRed()), 2);
		}
	}

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
}
