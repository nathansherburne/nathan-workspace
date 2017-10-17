package driver;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.opencv.core.Core;

import technology.tabula.TextElement;
import technology.tabula.TextStripper;
import technology.tabula.Utils;
import textProcessing.Block;
import textProcessing.Document;
import textProcessing.Line;
import textProcessing.Neighborhood;
import textProcessing.Word;

public class Driver {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) throws InvalidPasswordException, IOException {
		File test_pdf = new File("src/tests/resources/grid1.pdf");
		PDDocument pdfDocument = PDDocument.load(test_pdf);

		// For each page
		// Create Characters
		TextStripper textStripper = new TextStripper(pdfDocument, 1);
		textStripper.process();
		// if textelemtns > 0
		Utils.sort(textStripper.textElements);

		Document document = new Document();
		document.createWords(textStripper.textElements);
		document.createLines();
		document.createDummyLines();
		document.createBlocks();
		document.mergeIsolateBlocks();
		
		drawBlocksAndSave(pdfDocument, 0, document.getBlocks(), "src/tests/resources/sample2-draw.pdf");
	}
		
		
		
		

		// TableFinder -> rulings
		// -> TableExtractor
		// -> CSVWriter
	
	public static void drawBlocksAndSave(PDDocument doc, int pageNum, List<Block> blocks, String filepath) throws IOException {
		PDPage page = doc.getPage(0);
		PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND,true);
		contentStream.setNonStrokingColor(Color.DARK_GRAY);
		contentStream.setLineWidth(1.0f);
		float pageHeight = page.getCropBox().getHeight();
		
		for(Block b: blocks) {
			
			contentStream.addRect(b.x, pageHeight - b.getBottom(), b.width, b.height);
		}
		contentStream.stroke();
		contentStream.close();
		doc.save(filepath);
	}
}
