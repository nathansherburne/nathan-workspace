package driver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.opencv.core.Core;

import technology.tabula.TextElement;
import technology.tabula.TextStripper;
import technology.tabula.Utils;
import textProcessing.Block;
import textProcessing.Document;
import textProcessing.Line;
import textProcessing.MarginPoint;
import textProcessing.Neighborhood;
import textProcessing.ReferencePoint;
import textProcessing.Word;

public class Driver {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) throws InvalidPasswordException, IOException {
		File test_pdf = new File("src/tests/resources/grid1.pdf");
		PDDocument pdfDocument = PDDocument.load(test_pdf);
		final float LINE_SPACING_THRESHOLD = 4; // Multiplication factor for deciding whether there is a large gap
												// between two lines.
		// For each page
		// Create Characters
		TextStripper textStripper = new TextStripper(pdfDocument, 1);
		textStripper.process();
		// if textelemtns > 0
		Utils.sort(textStripper.textElements);

		// Create Words
		Word word = new Word(textStripper.textElements.get(0));
		Document document = new Document();
		for (int i = 1; i < textStripper.textElements.size(); i++) {
			TextElement te = textStripper.textElements.get(i);
			if (te.getText().equals(" ")) {
				continue;
			}
			if (te.getMinX() - word.getMaxX() < word.getWidthOfSpace() && te.verticallyOverlaps(word)) {
				word.add(te);
			} else {
				document.add(word);
				word = new Word(te);
			}
		}
		document.add(word);

		// Create Lines
		Line line = new Line(document.getWords().get(0));
		for (int i = 1; i < document.getWords().size(); i++) {
			Word w = document.getWords().get(i);
			if (w.verticallyOverlaps(line)) {
				line.add(w);
			} else {
				document.add(line);
				line = new Line(w);
			}
		}
		document.add(line);

		// Create dummy spacing lines
		ListIterator<Line> iterator = document.getLines().listIterator();
		if (iterator.hasNext()) {
			Line currentLine = iterator.next();
			while (iterator.hasNext()) {
				Line previousLine = currentLine;
				currentLine = iterator.next();
				if (Math.abs(currentLine.getTop() - previousLine.getBottom()) > LINE_SPACING_THRESHOLD
						* Math.min(currentLine.getHeight(), previousLine.getHeight())) {
					// Large space between lines.
					float top = previousLine.getBottom();
					float left = Math.min(currentLine.getLeft(), previousLine.getLeft());
					float right = Math.max(currentLine.getRight(), previousLine.getRight());
					float bottom = currentLine.getTop();
					iterator.add(new Line(top + 1, left + 1, right - left - 1, bottom - top - 1));
				}
			}
		}

		// Create Blocks
		for (int i = 0; i < document.getLines().size(); i++) {
			Line currentLine = document.getLines().get(i);
			for (Word w : currentLine.getWords()) {
				if (w.isExpanded())
					continue;
				document.add(document.createBlock(w, i));
			}
		}

		// Merge isolated blocks

		// Separate blocks into horizontal neighborhoods
		List<Neighborhood> neighborhoods = new ArrayList<Neighborhood>();
		ListIterator<Block> blockIterator = document.getBlocks().listIterator();
		if (blockIterator.hasNext()) {
			Block b = blockIterator.next();
			Neighborhood n = new Neighborhood(b);
			while (blockIterator.hasNext()) {
				b = blockIterator.next();
				if (b.horizontallyOverlaps(n)) {
					n.add(b);
				} else {
					neighborhoods.add(n);
					n = new Neighborhood(b);
				}
			}
			neighborhoods.add(n);
		}

		float RP_THRESHOLD = 1.0f;
		for (Neighborhood n : neighborhoods) {
			
			ReferencePoint rightRP = new ReferencePoint();
			Iterator<Float> keyIterator = n.getMarginStructure().keySet().iterator();
			float currentKey, previousKey;
			if (keyIterator.hasNext()) {
				currentKey = keyIterator.next();
				rightRP.add(n.getMarginStructure().get(currentKey));
			}
			while (keyIterator.hasNext()) {
				previousKey = currentKey;
				currentKey = keyIterator.next();
				if (currentKey - previousKey < RP_THRESHOLD) {
					rightRP.add(n.getMarginStructure().get(currentKey));
				} else {
					
				}
				MarginPoint mp = n.getMarginStructure().get(x);
				if (mp.isRight())
					continue;
				rightRPs.add(mp);
			}
		}

		// TableFinder -> rulings
		// -> TableExtractor
		// -> CSVWriter
	}
}
