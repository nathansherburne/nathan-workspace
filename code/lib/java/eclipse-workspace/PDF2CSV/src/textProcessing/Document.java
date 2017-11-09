package textProcessing;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import technology.tabula.TextElement;
import technology.tabula.TextStripper;
import technology.tabula.Utils;

public class Document {
	private double lineSpacingThreshold = 1.0f; // Multiplication factor for deciding whether there is a large gap											// between two lines.
	private double spaceScale = 1.0f; // Multiplication factor for multiplying the definition of width of space.
	private int pageNum = 0;

	private PDDocument pdfDocument;
	private List<TextElement> textElements = new ArrayList<TextElement>();
	private List<Line> lines = new ArrayList<Line>();
	private List<Block> blocks = new ArrayList<Block>();
	private List<Word> words = new ArrayList<Word>();
	private List<Neighborhood> neighborhoods = new ArrayList<Neighborhood>();
	
	public Document(PDDocument pdfDocument, Integer pageNum, Double lineSpacingThreshold, Double spaceScale) {
		this.pdfDocument = pdfDocument;
		if(pageNum != null) {
			this.pageNum = pageNum;
		}
		if(lineSpacingThreshold != null) {
			this.lineSpacingThreshold = lineSpacingThreshold;
		}
		if(spaceScale != null) {
			this.spaceScale = spaceScale;
		}
		init();
	}
	
	private void init() {
		extractText();
		createWords();
		createLines();
		createDummyLines();
		createBlocks();
		
		//isolateMergedColumns();
		//createNeighborhoods();
		//mergeIsolateBlocks();
		//decomposeType1Blocks();
	}

	public PDDocument getPDDocument() {
		return pdfDocument;
	}
	
	public int getPageNum() {
		return pageNum;
	}

	public void add(Word w) {
		words.add(w);
	}

	public void add(Line l) {
		lines.add(l);
	}

	public void add(Block b) {
		blocks.add(b);
	}

	public List<Word> getWords() {
		return words;
	}

	public List<Neighborhood> getNeighborhoods() {
		return neighborhoods;
	}
	
	public List<Line> getDummyLines() {
		List<Line> dummyLines = new ArrayList<>();
		for(Line line : lines) {
			if(line.getText().equals("")) {
				dummyLines.add(line);
			}
		}
		return dummyLines;
	}
	
	public List<Line> getLines() {
		return lines;
	}

	/**
	 * Right now, the blocks defined by this document's "blocks" variable are
	 * created by the "createBlocks" method, which uses the ovl() function to group
	 * words on succeeding and preceding lines recursively.
	 * 
	 * But after neighborhoods are created, they merge some blocks and separate
	 * others. This needs to be more unified (i.e. when a neighborhood re-defines a
	 * block, the "blocks" variable of this document should be updated).
	 * 
	 * For now, if there are no neighborhoods present, the blocks created by
	 * createBlocks(), will be returned. If there are neighborhoods created, though
	 * each neighborhood's blocks will be collected nd all of them returned.
	 * 
	 * @return
	 */
	public List<Block> getBlocks() {
		if (neighborhoods.isEmpty()) {
			return blocks;
		}
		List<Block> blocks = new ArrayList<Block>();
		for (Neighborhood n : neighborhoods) {
			blocks.addAll(n.getTextElements());
		}
		return blocks;
	}

	private Block createBlock(Word seed, int currentLineNumber) {
		Block block = new Block(seed);
		seed.setExpanded(true);

		List<Word> ovlAbove = new ArrayList<Word>();
		List<Word> ovlBelow = new ArrayList<Word>();

		if (currentLineNumber - 1 >= 0) { // Has previous line
			ovlAbove = getOvlWords(seed, getLines().get(currentLineNumber - 1));
		}
		if (currentLineNumber + 1 < getLines().size()) { // Has next line
			ovlBelow = getOvlWords(seed, getLines().get(currentLineNumber + 1));
		}

		for (Word ovlWord : ovlAbove) {
			if (ovlWord.isExpanded())
				continue;
			block.merge(createBlock(ovlWord, currentLineNumber - 1));
		}
		for (Word ovlWord : ovlBelow) {
			if (ovlWord.isExpanded())
				continue;
			block.merge(createBlock(ovlWord, currentLineNumber + 1));
		}
		return block;
	}

	private List<Word> getOvlWords(Word seed, Line succ) {
		List<Word> ovl = new ArrayList<Word>();
		for (Word check : succ.getWords()) {
			if (ovl(seed, check)) {
				ovl.add(check);
			}
		}
		return ovl;
	}

	private boolean ovl(Word w1, Word w2) {
		return w2.horizontallyOverlaps(w1);
	}

	/**
	 * Takes a sorted list of TextElements (characters) and groups them into words
	 * based on the width of a space character.
	 * 
	 */
	private void createWords() {
		Word word = new Word(textElements.get(0));
		for (int i = 1; i < textElements.size(); i++) {
			TextElement te = textElements.get(i);
			// Space characters can be disregarded because when the next character comes
			// around, the distance will have increased by the width of the space and the
			// next if statement will produce the desired effect.
			if (te.getText().equals(" ")) {
				continue;
			}
			if (Math.abs(te.getMinX() - word.getMaxX()) < word.getWidthOfSpace() * spaceScale
					&& te.verticallyOverlaps(word)) {
				word.add(te);
			} else {
				add(word);
				word = new Word(te);
			}
		}
		add(word);
	}
	
	private void extractText() {
		TextStripper textStripper = null;
		try {
			textStripper = new TextStripper(pdfDocument, pageNum + 1);
			textStripper.process();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (textStripper.textElements.size() == 0) {
			System.err.println("Could not extract text from the PDF.");
			System.exit(1);
		}
		// Sort and then combine characters into words (that's why characters must be
		// sorted, since
		// they will be evaluated sequentially).
		Utils.sort(textStripper.textElements);
		this.textElements = textStripper.textElements;
	}
	

	/**
	 * Groups the words in this document into lines.
	 */
	private void createLines() {
		Line line = new Line(getWords().get(0));
		for (int i = 1; i < getWords().size(); i++) {
			Word w = getWords().get(i);
			if (w.verticallyOverlaps(line)) {
				line.add(w);
			} else {
				add(line);
				line = new Line(w);
			}
		}
		add(line);
	}

	/**
	 * Inserts blank lines to represent large horizontal whitespace in a page. These
	 * dummy lines are important because they distinguish between lines that have no
	 * text between each other, but are visibly separate.
	 */
	private void createDummyLines() {
		ListIterator<Line> iterator = getLines().listIterator();
		if (iterator.hasNext()) {
			Line currentLine = iterator.next();
			while (iterator.hasNext()) {
				Line previousLine = currentLine;
				currentLine = iterator.next();
				if (Math.abs(currentLine.getTop() - previousLine.getBottom()) > lineSpacingThreshold
						* Math.min(currentLine.getHeight(), previousLine.getHeight())) {
					float top = previousLine.getBottom();
					float left = Math.min(currentLine.getLeft(), previousLine.getLeft());
					float right = Math.max(currentLine.getRight(), previousLine.getRight());
					float bottom = currentLine.getTop();
					// Do a little dance to get the dummy line in between previousLine and
					// currentLine.
					iterator.previous();
					iterator.add(new Line(top + 1, left + 1, right - left - 1, bottom - top - 1));
					iterator.next();
				}
			}
		}
	}

	/**
	 * Organize words into blocks based on the T-Recs table stucturing algorithm.
	 * See: https://www.dfki.uni-kl.de/~kieni/publications/LNCS_DAS98.pdf
	 */
	private void createBlocks() {
		for (int lineNumber = 0; lineNumber < getLines().size(); lineNumber++) {
			Line currentLine = getLines().get(lineNumber);
			for (Word w : currentLine.getWords()) {
				if (w.isExpanded())
					continue;
				add(createBlock(w, lineNumber));
			}
		}
	}

	/**
	 * Cluster blocks together with their horizontal neighbors.
	 */
	public void createNeighborhoods() {
		ListIterator<Block> blockIterator = blocks.listIterator();
		if (blockIterator.hasNext()) {
			Block b = blockIterator.next();
			Neighborhood n = new Neighborhood(b, spaceScale);
			while (blockIterator.hasNext()) {
				b = blockIterator.next();
				if (b.verticallyOverlaps(n)) {
					n.add(b);
				} else {
					neighborhoods.add(n);
					n = new Neighborhood(b, spaceScale);
				}
			}
			neighborhoods.add(n);
		}
	}
	
	/**
	 * Removes neighborhoods from the list of neighborhoods if they do not have 
	 * the minimum number of rows.
	 * @param minRows
	 */
	public void removeNonTableNeighborhoods(int minRows) {
		Iterator<Neighborhood> nIter = neighborhoods.iterator();
		while(nIter.hasNext()) {
			Neighborhood n = nIter.next();
			if(n.getHorizontalRulings().size() < minRows) {
				nIter.remove();
			}
		}
	}
	
	public void removeBlocksNotInROI(Rectangle2D roi) {
		Iterator<Neighborhood> nIter = neighborhoods.iterator();
		while (nIter.hasNext()) {
			Neighborhood n = nIter.next();
			Iterator<Block> bIter = n.getTextElements().iterator();
			while(bIter.hasNext()) {
				Block b = bIter.next();
				if(!roi.contains(b)) {
					bIter.remove();
				}
			}
			if(n.getTextElements().isEmpty()) { // All the blocks in this neighborhood were removed (i.e. outside of ROI).
				nIter.remove();
			}
		}
	}

	/**
	 * Replaces the current set of blocks with the set of merged blocks.
	 */
	public void mergeIsolateBlocks() {
		this.blocks = mergeIsolatedBlocks(neighborhoods);
	}

	private List<Block> mergeIsolatedBlocks(List<Neighborhood> neighborhoods) {
		List<Block> allMergedBlocks = new ArrayList<Block>();
		for (Neighborhood n : neighborhoods) {
			n.mergeIsolated();
			allMergedBlocks.addAll(n.getTextElements());
		}
		return allMergedBlocks;

	}

	/**
	 * Unification of Block Abstraction Level: Splits Type 1 blocks into lines (i.e.
	 * splits them into individual rows).
	 * 
	 * Problems: 1. Cells that have two or more words should be considered Type 1.
	 * 2. Cells that have two words on separate lines are decomposed.
	 */
	public void decomposeType1Blocks() {
		for (Neighborhood n : neighborhoods) {
			List<Block> blocks = n.getTextElements();
			List<Block> decomposedBlocks = new ArrayList<Block>();
			ListIterator<Block> iter = blocks.listIterator();
			while (iter.hasNext()) {
				Block block = iter.next();
				// check to see if block is Type 1, if so decompose
				switch (block.getType()) {
				case TYPE1:
					iter.remove();
					decomposedBlocks.addAll(block.decompose());
					break;
				case TYPE2:
					break;
				}
			}
			blocks.addAll(decomposedBlocks);
		}
	}
	
	/**
	 * Merges type 2 blocks that are not significantly more than one space apart.
	 * 
	 * DOES NOT WORK YET.
	 */
//	public void removeRivers() {
//		for(Neighborhood n : neighborhoods) {
//			Collections.sort(n.getTextElements());
//			ListIterator<Block> bIter = n.getTextElements().listIterator();
//			while(bIter.hasNext()) {
//				Block current = bIter.next();
//				if(current.getType() != Block.BLOCK_TYPE.TYPE2) {
//					continue;
//				}
//				Block leftNeighbor = n.getClosestLeftNeighbor(current);
//				if(leftNeighbor == null || leftNeighbor.getType() != Block.BLOCK_TYPE.TYPE2) {
//					continue;
//				}
//				float distance = Math.abs(n.horizontalOverlapValue(current, leftNeighbor));
//				float widthOfSpace = Math.max(current.getAvgWidthOfSpace(), leftNeighbor.getAvgWidthOfSpace());
//				if(distance <= widthOfSpace) {
//					bIter.remove();
//					leftNeighbor.merge(current);
//				}
//			}
//		}
//	}

	public void isolateMergedColumns() {
		ListIterator<Block> bIter = blocks.listIterator();
		while (bIter.hasNext()) {
			Block block = bIter.next();
			switch (block.getType()) {
			case TYPE1:
				break;
			case TYPE2:
				block.setAllWordsExpanded(false);
				List<Line> lines = block.getLines();
				for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
					Line currentLine = lines.get(lineNumber);

					for (Word w : currentLine.getWords()) {
						if (w.isExpanded()) {
							continue;
						}
						List<Word> splitted_sons = getSplittedSons(w, lines, lineNumber);
						if (splitted_sons.size() > 1) {
							Iterator<Word> iter = splitted_sons.iterator();
							Word son = iter.next();
							Block splSonBlock = new Block(son);
							while (iter.hasNext()) {
								son = iter.next();
								splSonBlock.add(son);
							}
							block.removeAll(splitted_sons);
							bIter.add(splSonBlock);
						}
					}
				}
			}
		}
	}

	public List<Word> getSplittedSons(Word seed, List<Line> lines, int currentLineNum) {
		List<Word> ovlBelow;
		List<Word> ovlAbove;
		List<Word> splitted_sons = new ArrayList<Word>();

		splitted_sons.add(seed);
		seed.setExpanded(true);
		if (currentLineNum + 1 >= lines.size()) { // Does not have next line.
			return splitted_sons;
		}
		Line lineBelow = lines.get(currentLineNum + 1);
		Line currentLine = lines.get(currentLineNum);

		ovlBelow = getOvlWords(seed, lineBelow);
		if (ovlBelow.size() == 1) { // Only one neighbor below.
			Word singleNeighborBelow = ovlBelow.get(0);
			ovlAbove = getOvlWords(singleNeighborBelow, currentLine);
			if (ovlAbove.size() == 1) { // Only one neighbor above.
				Word singleNeighborAbove = ovlAbove.get(0);
				if (singleNeighborAbove.equals(seed)) {
					// Both words agree. Put them in split sons group and remove from this block.
					splitted_sons.addAll(getSplittedSons(singleNeighborBelow, lines, currentLineNum + 1));
				}
			}
		}
		return splitted_sons;
	}

	public String getTableString(int neighborhoodNum) {
		return neighborhoods.get(neighborhoodNum).getTableString();
	}

	public void printWords() {
		for (Word w : words) {
			System.out.println(w.getText());
			System.out.println(w.getBounds());
		}
	}

	public BufferedImage getDocumentImage() {
		PDFRenderer pdfRenderer = new PDFRenderer(getPDDocument());
		BufferedImage bim = null;
		try {
			bim = pdfRenderer.renderImageWithDPI(pageNum, 300, ImageType.RGB);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bim;
	}
}
