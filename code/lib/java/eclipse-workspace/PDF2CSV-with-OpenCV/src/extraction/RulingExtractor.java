package extraction;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.TextPosition;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import image_processing.ImageProcessor;
import technology.tabula.Ruling;
import technology.tabula.TextElement;
import technology.tabula.TextStripper;
import tests.DrawPrintTextLocations;
import utils.MyUtils;
import utils.RemoveAllText;

public class RulingExtractor {
	private final PDDocument pdfDocument;
	protected List<Ruling> rulings;
	private ImageProcessor imageProcessor;
	private TextStripper pdfTextStripper;

	public RulingExtractor(PDDocument pdfDocument, ImageProcessor imageProcessor, TextStripper pdfTextStripper) {
		this.pdfDocument = pdfDocument;
		this.rulings = new ArrayList<>();
		this.imageProcessor = imageProcessor;
		this.pdfTextStripper = pdfTextStripper;
	}

	public void processPage(int pageNumber) throws IOException {
		
		


		List<TextElement> texts = pdfTextStripper.textElements;

		// Process text for additional (implied) rulings.

		// Determine if rulings are part of a table or not

	}

	public List<Ruling> findPhysicalRulings(int pageNumber) throws IOException {
		PDDocument noTextDoc = getNoTextPage(pageNumber);
		BufferedImage image = pageToImage(pageNumber, noTextDoc);
		Mat img = imageProcessor.img2Mat(image);
		
		List<Line2D.Float> verticalRulings = imageProcessor.getVerticalRulings(img);
		List<Line2D.Float> horizontalRulings = imageProcessor.getHorizontalRulings(img);

		List<Line2D.Float> rulings = Stream.concat(horizontalRulings.stream(), verticalRulings.stream()).collect(Collectors.toList());
		
		PDPage page = pdfDocument.getPage(pageNumber);
		List<Ruling> rulings1 = scaleRulings(page, image, rulings);
		return rulings1;
	}

	public void findImpliedRulings(int pageNumber) throws IOException {
		BufferedImage image = pageToImage(pageNumber, pdfDocument);
		Mat img = imageProcessor.img2Mat(image);
		
		// Get string location bounding rectangles
		DrawPrintTextLocations dptl = new DrawPrintTextLocations(pdfDocument);
		dptl.process(pageNumber);

		Rectangle2D pdfDimension = new Rectangle2D.Double(0, 0, pdfDocument.getPage(pageNumber).getBBox().getWidth(), 
				pdfDocument.getPage(pageNumber).getBBox().getHeight());
		Rectangle2D imageDimension = new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight());
		List<Rect> scaledBboxes = new ArrayList<Rect>();
		
		List<TextElement> t = pdfTextStripper.textElements;
		
		
		for(Rectangle2D rect : dptl.getStringBboxes()) {
			Rectangle2D scaledRect = points2Rect(scaleRectangle(rect, pdfDimension, imageDimension, pdfDocument.getPage(pageNumber).getRotation()));
			scaledBboxes.add(new Rect((int) scaledRect.getX(), (int) scaledRect.getY(), (int) scaledRect.getWidth(), (int) scaledRect.getHeight()));
		}
		
		List<Line2D.Float> impliedRulings = imageProcessor.getImpliedRulings(img, scaledBboxes);
		//imageProcessor.getRegions(blobs.submat());
		
		// For each rectangle, blobify the submat of the image horizontally/vertically
		// Could use "space width" for horizontal scale
		// Vertically blob the while image
		// Instead of blobbing whole image at once, could use each rectangle's string
		// height to blob one text at a time.

	}

	private BufferedImage pageToImage(int pageNumber, PDDocument doc) throws IOException {
		PDFRenderer pdfRenderer = new PDFRenderer(doc);
		return pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
	}

	/**
	 * 
	 * @param pageNumber
	 * @return a PDDocument with one page, specified by pageNumber, with all the
	 *         text removed.
	 * @throws IOException
	 */
	private PDDocument getNoTextPage(int pageNumber) throws IOException {
		PDDocument noTextDoc = new PDDocument();
		noTextDoc.importPage(pdfDocument.getPage(pageNumber));
		RemoveAllText textRemover = new RemoveAllText(noTextDoc);
		return textRemover.getNoTextDocument();
	}

	private List<Ruling> scaleRulings(PDPage page, BufferedImage image, List<Line2D.Float> rulings) {
		List<Ruling> scaledRulings = new ArrayList<Ruling>();
		int rotation = page.getRotation();
		Rectangle2D currentDim = new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight());
		Rectangle2D resultDim = new Rectangle2D.Double(0, 0, page.getBBox().getWidth(), page.getBBox().getHeight());
		for (Line2D.Float ruling : rulings) {
			Pair<Point2D> scaledPoints = scaleLine(ruling, currentDim, resultDim, rotation);
			scaledRulings.add(new Ruling(scaledPoints.getItem1(), scaledPoints.getItem2()));
		}
		return scaledRulings;
	}
	
	private Pair<Point2D> scaleRectangle(Rectangle2D rect, Rectangle2D currentDim, Rectangle2D scaledDim, int rotation) {
		Point2D.Double upperLeft = new Point2D.Double(rect.getX(), rect.getY());
		Point2D.Double lowerRight = new Point2D.Double(rect.getMaxX(), rect.getMaxY());
		return scalePoints2D(new Pair<Point2D>(upperLeft, lowerRight), currentDim, scaledDim, rotation);	
	}
	
	/**
	 * Converts a pair of points into a rectangle.
	 * @param corners upper-left and lower-right corners of the desired rectangle.
	 * @return
	 */
	private Rectangle2D points2Rect(Pair<Point2D> corners) {
		double x = corners.getItem1().getX();
		double y = corners.getItem1().getY();
		double w = corners.getItem2().getX() - x;
		double h = corners.getItem2().getY() - y;
		return new Rectangle2D.Double(x, y, w, h);
	}
		
	private Pair<Point2D> scaleLine(Line2D line, Rectangle2D currentDim, Rectangle2D scaledDim, int rotation) {
		Point2D.Double p1 = new Point2D.Double(line.getX1(), line.getY1());
		Point2D.Double p2 = new Point2D.Double(line.getX2(), line.getY2());
		return scalePoints2D(new Pair<Point2D>(p1, p2), currentDim, scaledDim, rotation);
		
	}
	
	private Point2D scalePoint(Point2D p, double vscale, double hscale) {
		return new Point2D.Double(p.getX() * hscale, p.getY() * vscale); 
	}
	
	private Pair<Point2D> scalePoints2D(Pair<Point2D> points, Rectangle2D currentDim, Rectangle2D scaledDim, int rotation) {
		double vscale, hscale;
		if (rotation == 90 || rotation == 270) {
			vscale =  scaledDim.getHeight() / currentDim.getWidth();
			hscale =  scaledDim.getWidth() / currentDim.getHeight();
		} else {
			vscale = currentDim.getHeight() / scaledDim.getHeight();
			hscale = currentDim.getWidth() / scaledDim.getWidth();
		}
		
		return new Pair<Point2D>(scalePoint(points.getItem1(), vscale, hscale), 
				scalePoint(points.getItem2(), vscale, hscale));
	}
	
	private class Pair<X> {
		private X item1;
		private X item2;
		
		private Pair(X item1, X item2) {
			this.item1 = item1;
			this.item2 = item2;
		}
		
		public X getItem1() {
			return item1;
		}
		
		public X getItem2() {
			return item2;
		}
	}
}
