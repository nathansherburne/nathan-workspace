package tests;

import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import image_processing.ImageProcessor;
import technology.tabula.Rectangle;
import utils.MyUtils;
import utils.RemoveAllText;

public class TableDetectionTester {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	public static void main(String[] args) throws InvalidPasswordException, IOException {
		File test_pdf = new File("src/tests/resources/mixed1.pdf");
		int test_page = 0;
		
		PDDocument document = PDDocument.load(test_pdf);
		RemoveAllText textRemover = new RemoveAllText(document);
		PDDocument doc_no_text = textRemover.getNoTextDocument();
		PDFRenderer pdfRenderer = new PDFRenderer(doc_no_text);
		BufferedImage bim = pdfRenderer.renderImageWithDPI(test_page, 300, ImageType.RGB);
		doc_no_text.close();
		document.close();
		ImageProcessor proc = new ImageProcessor(bim);
		//MyUtils.displayImage(MyUtils.resize(proc.gray(), 2000, 1200));
		//MyUtils.displayImage(MyUtils.resize(proc.original(), 2000, 1200));
		MyUtils.displayImage(MyUtils.resize(proc.mask(), 1200, 800));
		//MyUtils.displayImage(MyUtils.resize(proc.hough(), 1200, 800));
		List<Line2D.Float> rulings = proc.getRulings();
		Mat image = proc.getImage();
		PDPage page = document.getPage(test_page);
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
			
			Line2D.Float scaledL = new Line2D.Float(x1, y1, x2, y2);
			scaledRulings.add(scaledL);
		}
//		BufferedImage test = null;
//		try {
//		    test = ImageIO.read(new File("src/tests/resources/line.jpg"));
//		} catch (IOException e) {
//		}
//		ImageProcessor proc = new ImageProcessor(test);
//		MyUtils.displayImage(proc.comp());
//		MyUtils.displayImage(proc.bh());
	}

}
