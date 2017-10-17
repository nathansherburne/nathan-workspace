package tests;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

public class Test {

	public static void main(String[] args) throws InvalidPasswordException, IOException {
		File test_pdf = new File("src/tests/resources/sample-tables.pdf");
		PDDocument pdfDocument = PDDocument.load(test_pdf);
		pdfDocument.setAllSecurityToBeRemoved(true);
		int pageToKeep = 1;
		int origPageNum = pageToKeep;
		for(int i = 0; i < pdfDocument.getNumberOfPages(); i++) {
			System.out.println(pdfDocument.getNumberOfPages());

			if(i != pageToKeep) {
				pdfDocument.removePage(i--);
				pageToKeep--;
			}
		}

		pdfDocument.save("src/tests/resources/sample1-page" + origPageNum + ".pdf");
		pdfDocument.close();
	}
	
	
	
	private static final Color[] KELLY_COLORS = { 
			Color.decode("0xFFB300"), // Vivid Yellow
			Color.decode("0x803E75"), // Strong Purple
			Color.decode("0xFF6800"), // Vivid Orange
			Color.decode("0xA6BDD7"), // Very Light Blue
			Color.decode("0xC10020"), // Vivid Red
			Color.decode("0xCEA262"), // Grayish Yellow
			Color.decode("0x817066"), // Medium Gray

			Color.decode("0x007D34"), // Vivid Green
			Color.decode("0xF6768E"), // Strong Purplish Pink
			Color.decode("0x00538A"), // Strong Blue
			Color.decode("0xFF7A5C"), // Strong Yellowish Pink
			Color.decode("0x53377A"), // Strong Violet
			Color.decode("0xFF8E00"), // Vivid Orange Yellow
			Color.decode("0xB32851"), // Strong Purplish Red
			Color.decode("0xF4C800"), // Vivid Greenish Yellow
			Color.decode("0x7F180D"), // Strong Reddish Brown
			Color.decode("0x93AA00"), // Vivid Yellowish Green
			Color.decode("0x593315"), // Deep Yellowish Brown
			Color.decode("0xF13A13"), // Vivid Reddish Orange
			Color.decode("0x232C16"), // Dark Olive Green
	};
}
