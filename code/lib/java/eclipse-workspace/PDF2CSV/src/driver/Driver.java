package driver;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.opencv.core.Core;


public class Driver {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) throws InvalidPasswordException, IOException {
		File test_pdf = new File("src/tests/resources/grid1.pdf");
		PDDocument document = PDDocument.load(test_pdf);
		
		// For each page
		//    TableFinder -> rulings
		// -> TableExtractor
		// -> CSVWriter
		
	}
}
