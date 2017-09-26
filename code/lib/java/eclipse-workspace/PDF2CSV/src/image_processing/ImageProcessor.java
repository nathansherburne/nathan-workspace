package image_processing;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import utils.MyUtils;

public class ImageProcessor {

	int hole_scale, line_scale;
	
	public ImageProcessor() throws IOException {
		this.hole_scale = 300;
		this.line_scale = 20;
	}
	
	public ImageInfo getInfo(Mat image) {
		// Some images have white lines on black background. Other images have black
		// lines on white backgrounds. Use Canny edge detector to make lines white no
		// matter what the background and line colors are. The important part is not
		// edge detection, it is making the edges white.
		// Blur to connect corners.
		Mat edges = canny(image, 50, 150, 3, true);
		Mat blur = gaussianBlur(edges, new Size(11.0, 11.0), 0.0);
		Mat bw = adaptiveThreshold(blur, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 0);
		Mat closed = close(bw, hole_scale);
		Mat horizontal = getHorizontalLines(closed, line_scale);
		Mat vertical = getVerticalLines(closed, line_scale);
		ImageInfo imageInfo = new ImageInfo(image);
		imageInfo.addBinary(bw);
		imageInfo.addHorizontalLines(horizontal);
		imageInfo.addVerticalLines(vertical);
		return imageInfo;
	}

	/**
	 * 
	 * @param in
	 * @return a Mat which was converted from image in.
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

	public Mat fill(Mat image, List<Rect> rects) {
		int i = 0;
		for (Rect rect : rects) {
			// System.out.println(i++ + ": " + "[" + rect.x + ", " + rect.y + ", " +
			// rect.width + ", " + rect.height + "]");
			// System.out.println(image.cols() + ", " + image.rows());
			image = fill(image, rect);
		}
		return image;
	}

	public Mat fill(Mat image, Rect rect) {
		Mat submat = image.submat(rect);
		submat.setTo(new Scalar(255, 255, 255));
		return image;
	}

	/**
	 * Connects colinear line segments.
	 * 
	 * @param bw
	 *            a binary Mat, usually of horizontal and vertical lines only.
	 * @param hole_scale
	 *            the proportion of the width (for horizontal lines) or the height
	 *            (for vertical lines) of the page of the biggest gap between line
	 *            segments. For example, a hole_scale of 100 on a 200x100 page means
	 *            that any hole will at most the size of 2x1 pixels. If the
	 *            hole_scale is too small, unrelated components of the image will be
	 *            blobbed together.
	 * @return
	 */
	public Mat close(Mat bw, int hole_scale) {
		return verticalClose(horizontalClose(bw, hole_scale), hole_scale);
	}

	public Mat horizontalClose(Mat bw, int hole_scale) {
		Mat closed = bw.clone();
		int horizontalsize = closed.cols() / hole_scale;
		Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horizontalsize, 1));
		Imgproc.dilate(closed, closed, horizontalStructure);
		Imgproc.erode(closed, closed, horizontalStructure);

		return closed;
	}

	public Mat verticalClose(Mat bw, int hole_scale) {
		Mat closed = bw.clone();
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
	public Mat getHorizontalLines(Mat bw, int line_scale) {
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
	public Mat getVerticalLines(Mat bw, int line_scale) {
		Mat vertical = bw.clone();
		int verticalsize = vertical.rows() / line_scale;
		Mat verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, verticalsize));
		Imgproc.erode(vertical, vertical, verticalStructure);
		Imgproc.dilate(vertical, vertical, verticalStructure);
		return vertical;
	}

	/**
	 * Finds the connected components of the image and creates a Region for each
	 * one.
	 * 
	 * @param image
	 * @return an array of Regions that are connected components.
	 */
	public Region[] getRegions(Mat image) {
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

	/**
	 * Finds the Region for each connected component of the image. This is why the
	 * input image must consist of disconnected lines, so that each one will have
	 * it's own Region.
	 * 
	 * @param disconnectedLines
	 *            an image of disconnected lines
	 * @return a list of lines describing the lines in the input image.
	 */
	public List<Line2D.Float> getLines(Mat disconnectedLines) {
		List<Line2D.Float> lines = new ArrayList<Line2D.Float>();

		// Since the input image is supposed to contain a bunch of disconnected lines,
		// finding "connected components" will actually find the coordinates of the
		// lines, each line being it's own connected component.

		Region[] lineRegions = getRegions(disconnectedLines);
		for (int i = 0; i < lineRegions.length; i++) {
			lines.add(rectToLine(lineRegions[i].getBounding()));
		}
		return lines;
	}

	/**
	 * Converts a rectangle into a line.
	 * 
	 * @param r
	 * @return the line that is "spine" of the rectangle. Direction depends on the
	 *         larger dimension of the rectangle.
	 */
	private Line2D.Float rectToLine(Rect r) {
		if (isHorizontal(r)) {
			float avgY = (r.y + (r.y + r.height)) / 2;
			return new Line2D.Float(new Point2D.Float(r.x, avgY), new Point2D.Float(r.x + r.width, avgY));
		}
		float avgX = (r.x + (r.x + r.width)) / 2;
		return new Line2D.Float(new Point2D.Float(avgX, r.y), new Point2D.Float(avgX, r.y + r.height));
	}

	private boolean isHorizontal(Rect r) {
		return r.width > r.height;
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

	public Mat canny(Mat image, double threshold1, double threshold2, int apertureSize, boolean L2gradient) {
		Mat edges = new Mat();
		Imgproc.Canny(image, edges, threshold1, threshold2, apertureSize, L2gradient);
		return edges;
	}

	public Mat gaussianBlur(Mat src, Size ksize, double sigmaX) {
		Mat blur = new Mat();
		Imgproc.GaussianBlur(src, blur, ksize, sigmaX);
		return blur;
	}

	public Mat adaptiveThreshold(Mat src, double maxValue, int adaptiveMethod, int thresholdType, int blockSize,
			double C) {
		Mat bw = new Mat();
		Imgproc.adaptiveThreshold(src, bw, maxValue, adaptiveMethod, thresholdType, blockSize, C);
		return bw;
	}

	public Mat bitwise_or(Mat image1, Mat image2) {
		Mat result = new Mat();
		Core.bitwise_or(image1, image2, result);
		return result;
	}

	// // Saving for later in case I want to use it.
	// public void houghlines() {
	// List<Line2D.Float> rulings = new ArrayList<Line2D.Float>();
	// Mat lines = new Mat();
	// Imgproc.HoughLinesP(mask, lines, 3, Math.PI / 180, 50, 60, 10);
	// hough = new Mat(mask.rows(), mask.cols(), CvType.CV_8UC3);
	// int colorNum = 0;
	// Color color = MyUtils.KELLY_COLORS[colorNum++];
	// for (int i = 0; i < lines.rows(); i++) {
	// double[] val = lines.get(i, 0);
	// rulings.add(new Line2D.Float(new Point2D.Float((float) val[0], (float)
	// val[1]),
	// new Point2D.Float((float) val[2], (float) val[3])));
	// Point p1 = new Point(val[0], val[1]);
	// Point p2 = new Point(val[2], val[3]);
	// for (int j = 0; j < regions.length; j++) {
	// if (regions[j].getBounding().contains(p1) &&
	// regions[j].getBounding().contains(p2)) {
	// color = MyUtils.KELLY_COLORS[j];
	// }
	// }
	// Imgproc.line(hough, p1, p2, new Scalar(color.getBlue(), color.getGreen(),
	// color.getRed()), 2);
	// }
	// }
}
