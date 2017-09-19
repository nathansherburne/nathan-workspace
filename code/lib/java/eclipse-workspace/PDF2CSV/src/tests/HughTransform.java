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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import image_processing.ImageProcessor;
import utils.MyUtils;
import utils.RemoveAllText;
public class HughTransform {
	
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) throws InvalidPasswordException, IOException {
		File test_pdf = new File("src/tests/resources/2017_weekly.pdf");
		int test_page = 0;
		
		PDDocument document = PDDocument.load(test_pdf);
		RemoveAllText textRemover = new RemoveAllText(document);
		PDDocument doc_no_text = textRemover.getNoTextDocument();
		PDFRenderer pdfRenderer = new PDFRenderer(doc_no_text);
		BufferedImage bim = pdfRenderer.renderImageWithDPI(test_page, 300, ImageType.RGB);
		doc_no_text.close();
		document.close();
		ImageProcessor proc = new ImageProcessor(bim);
	}
	
	public static void simpleLineTest() throws IOException {
		BufferedImage hline = null;
		try {
		    hline = ImageIO.read(new File("src/tests/resources/Horiz-line.jpg"));
		} catch (IOException e) {
		}
		ImageProcessor proc = new ImageProcessor(hline);
		//MyUtils.displayImage(MyUtils.toBufferedImage(houghTransform(proc)));
	}
}
