package tests;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.opencv.core.Core;

import image_processing.ImageProcessor;
import utils.MyUtils;
import utils.RemoveAllText;

public class TableDetectionTester {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	public static void main(String[] args) throws InvalidPasswordException, IOException {
		File test_pdf = new File("src/tests/resources/grid2.pdf");
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
		//MyUtils.displayImage(MyUtils.resize(proc.blurred(), 2000, 1200));
		//MyUtils.displayImage(MyUtils.resize(proc.mask(), 2000, 1200));
		MyUtils.displayImage(proc.bh());
		BufferedImage white = null;
		try {
		    white = ImageIO.read(new File("src/tests/resources/white_image.jpg"));
		} catch (IOException e) {
		}
		//proc = new ImageProcessor(white);
		//MyUtils.displayImage(MyUtils.resize(proc.original(), 2500, 1500));
		//MyUtils.displayImage(MyUtils.resize(proc.bw(), 2500, 1500));
	}

}
