package tests;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import extraction.RulingExtractor;
import image_processing.ImageProcessor;
import technology.tabula.TextStripper;
import technology.tabula.Utils;
import utils.MyUtils;
import utils.RemoveAllText;

public class TableDetectionTester {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	public static void main(String[] args) throws InvalidPasswordException, IOException {
		File test_pdf = new File("src/tests/resources/grid1.pdf");
		int test_page = 0;
		
		PDDocument document = PDDocument.load(test_pdf);
		PDDocument noTextDoc = new PDDocument();
		noTextDoc.importPage(document.getPage(test_page));
		RemoveAllText textRemover = new RemoveAllText(noTextDoc);
		noTextDoc = textRemover.getNoTextDocument();
		
		PDFRenderer pdfRenderer = new PDFRenderer(noTextDoc);
		BufferedImage bim = pdfRenderer.renderImageWithDPI(test_page, 300, ImageType.RGB);
		ImageProcessor proc = new ImageProcessor();
		
		
		
		TextStripper pdfTextStripper = new TextStripper(document, test_page+1);
        pdfTextStripper.process();
        Utils.sort(pdfTextStripper.textElements);
        //System.out.println(pdfTextStripper.textElements.size());
		RulingExtractor tester = new RulingExtractor(document, proc, pdfTextStripper);
		tester.findImpliedRulings(test_page);
		//Mat[] images = tester.processImage(bim, 300, 20);
//		for(int i = images.length - 1; i >= 0 ; i--) {
//			MyUtils.displayImage(MyUtils.resize(MyUtils.toBufferedImage(images[i]), 1200, 800));
//		}
	}
}
