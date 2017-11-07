package tests;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.opencv.core.Core;

import technology.tabula.TextElement;
import technology.tabula.TextStripper;
import technology.tabula.Utils;


public class TextStripperTester {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	public static void main(String[] args) throws InvalidPasswordException, IOException {
		File test_pdf = new File("src/tests/resources/grid1.pdf");		
		PDDocument document = PDDocument.load(test_pdf);
		TextStripper pdfTextStripper = new TextStripper(document, 1);
		pdfTextStripper.process();
		Utils.sort(pdfTextStripper.textElements);
	}
	
}
