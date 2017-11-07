package tests;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import image_processing.ImageProcessor;
import utils.MyUtils;
import utils.RemoveAllText;

// Used for finding lines amongst DISCONNECTED points.
public class HughTransform {

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
		ImageProcessor proc = new ImageProcessor();
		//BufferedImage test2 = proc.hough();
		
		// load the image
//		BufferedImage test = null;
//		try {
//			test = ImageIO.read(new File("src/tests/resources/line.jpg"));
//		} catch (IOException e) {
//		}
//		if (test2.getType() != BufferedImage.TYPE_3BYTE_BGR) {
//			// in = utils.MyUtils.convertTo3BYTE_BGR(in);
//			test2 = MyUtils.toBufferedImageOfType(test2, BufferedImage.TYPE_3BYTE_BGR);
//		}
//		byte[] pixels = ((DataBufferByte) test2.getRaster().getDataBuffer()).getData();
//		Mat img = new Mat(test2.getHeight(), test2.getWidth(), CvType.CV_8UC3);
//		img.put(0, 0, pixels);
//
//		// generate gray scale and blur
//		Mat gray = new Mat();
//		Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
//		Imgproc.blur(gray, gray, new Size(3, 3));
//		// detect the edges
//		Mat edges = new Mat();
//		int lowThreshold = 50;
//		int ratio = 3;
//		Imgproc.Canny(gray, edges, lowThreshold, lowThreshold * ratio);
//		MyUtils.displayImage(MyUtils.resize(MyUtils.toBufferedImage(edges), 2000, 1200));
//
//		Mat lines = new Mat();
//		Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 50, 20, 20);
//	    System.out.println(lines.rows());
//
//		for (int i = 0; i < lines.rows(); i++) {
//			double[] val = lines.get(i, 0);
//			Imgproc.line(img, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 0, 255), 2);
//		}
//		MyUtils.displayImage(MyUtils.resize(MyUtils.toBufferedImage(img), 2000, 1200));
	}
}
