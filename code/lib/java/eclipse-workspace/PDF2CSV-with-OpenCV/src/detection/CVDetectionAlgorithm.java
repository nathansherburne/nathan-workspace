package detection;

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
import technology.tabula.Page;
import technology.tabula.Rectangle;
import technology.tabula.detectors.DetectionAlgorithm;
import utils.RemoveAllText;

public class CVDetectionAlgorithm implements DetectionAlgorithm {

	@Override
	public List<Rectangle> detect(Page page) {
		List<Rectangle> tabula_tables = new ArrayList<Rectangle>();
		try {
			PDPage pdpage = page.getPDPage();
			//int page_num = page.getPageNumber() - 1; // Is this necessary??
			PDDocument document = new PDDocument();
			document.importPage(pdpage);
			RemoveAllText textRemover = new RemoveAllText(document);
			PDDocument noTextDoc = textRemover.getNoTextDocument();
			PDFRenderer pdfRenderer = new PDFRenderer(noTextDoc);
			BufferedImage buf_image;
			buf_image = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
			document.close();
			noTextDoc.close();
			ImageProcessor loc = new ImageProcessor();
			//List<Rect> cv_tables = loc.getTableRects();
			//Mat image = loc.getImage();

//			float ver_trans, hor_trans;
//			int rotation = page.getRotation();
//			if (rotation == 90 || rotation == 270) {
//				ver_trans = pdpage.getBBox().getWidth() / image.height();
//				hor_trans = pdpage.getBBox().getHeight() / image.width();
//			} else {
//				ver_trans = pdpage.getBBox().getHeight() / image.height();
//				hor_trans = pdpage.getBBox().getWidth() / image.width();
//			}
//
//			float tab_height, tab_width, tab_left, tab_top;
//			for (Rect table : cv_tables) {
//				tab_height = table.height * ver_trans;
//				tab_width = table.width * hor_trans;
//				tab_left = table.x * hor_trans;
//				tab_top = table.y * ver_trans;
//				Rectangle r = new Rectangle(tab_top, tab_left, tab_width, tab_height);
//				tabula_tables.add(r);
//			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return tabula_tables;
	}
}
