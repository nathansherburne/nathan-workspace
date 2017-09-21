package extraction;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import image_processing.ImageProcessor;
import technology.tabula.Ruling;
import utils.RemoveAllText;

public class RulingExtractor {
	private final PDDocument pdfDocument;
	protected List<Ruling> rulings;
	
	public RulingExtractor(PDDocument pdfDocument) {
		this.pdfDocument= pdfDocument;
		this.rulings = new ArrayList<>();
	}
	
	public void processPage(int pageNumber) throws IOException {
		
		// Process image for rulings.
		PDDocument noTextDoc = new PDDocument();
		noTextDoc.importPage(pdfDocument.getPage(pageNumber));
		RemoveAllText textRemover = new RemoveAllText(noTextDoc);
		noTextDoc = textRemover.getNoTextDocument();
		PDFRenderer pdfRenderer = new PDFRenderer(noTextDoc);
		BufferedImage buf_image;
		buf_image = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
		//pdfDocument.close();
		//noTextDoc.close();
		ImageProcessor loc = new ImageProcessor(buf_image);
		List<Rect> cv_tables = loc.getTableRects();
		Mat image = loc.getImage();
		
		List<Line2D.Float> rulings = loc.getRulings();
		PDPage page = pdfDocument.getPage(pageNumber);
		float ver_trans, hor_trans;
		int rotation = page.getRotation();
		if (rotation == 90 || rotation == 270) {
			ver_trans = page.getBBox().getWidth() / image.height();
			hor_trans = page.getBBox().getHeight() / image.width();
		} else {
			ver_trans = page.getBBox().getHeight() / image.height();
			hor_trans = page.getBBox().getWidth() / image.width();
		}

		List<Line2D.Float> scaledRulings = new ArrayList<Line2D.Float>();
		float x1, y1, x2, y2;
		for (Line2D.Float ruling : rulings) {
			x1 = ruling.x1 * hor_trans;
			y1 = ruling.y1 * ver_trans;
			x2 = ruling.x2 * hor_trans;
			y2 = ruling.y2 * ver_trans;
			
			//Line2D.Float scaledL = new Line2D.Float(x1, y1, x2, y2);
			this.rulings.add(new Ruling(new Point2D.Float(x1, y1), new Point2D.Float(x2,y2)));
		}
		// Process text for additional (implied) rulings.
	}
}
