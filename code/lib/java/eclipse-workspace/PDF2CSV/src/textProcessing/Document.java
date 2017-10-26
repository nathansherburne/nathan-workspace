package textProcessing;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import technology.tabula.TextElement;
import textProcessing.Block.BLOCK_TYPE;

public class Document {
	final float LINE_SPACING_THRESHOLD = 1.4f; // Multiplication factor for deciding whether there is a large gap
	// between two lines.

	List<Line> lines = new ArrayList<Line>();
	List<Block> blocks = new ArrayList<Block>();
	List<Word> words = new ArrayList<Word>();
	List<Neighborhood> neighborhoods = new ArrayList<Neighborhood>();

	public Document() {
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

	public List<Block> getBlocks() {
		List<Block> blocks = new ArrayList<Block>();
		for (Neighborhood n : neighborhoods) {
			blocks.addAll(n.getTextElements());
		}
		return blocks;
	}

	public List<Line> getLines() {
		return lines;
	}

	public Block createBlock(Word seed, int currentLineNumber) {
		Block block = new Block(seed);
		seed.setExpanded();

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
	 * @param textElements
	 *            a sorted list of TextElements
	 */
	public void createWords(ArrayList<TextElement> textElements) {
		Word word = new Word(textElements.get(0));
		for (int i = 1; i < textElements.size(); i++) {
			TextElement te = textElements.get(i);
			// Space characters can be disregarded because when the next character comes
			// around, the distance will have increased by the width of the space and the
			// next if statement will produce the desired effect.
			if (te.getText().equals(" ")) {
				continue;
			}
			if (Math.abs(te.getMinX() - word.getMaxX()) < word.getWidthOfSpace() && te.verticallyOverlaps(word)) {
				word.add(te);
			} else {
				add(word);
				word = new Word(te);
			}
		}
		add(word);
	}

	/**
	 * Groups the words in this document into lines.
	 */
	public void createLines() {
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
	public void createDummyLines() {
		ListIterator<Line> iterator = getLines().listIterator();
		if (iterator.hasNext()) {
			Line currentLine = iterator.next();
			while (iterator.hasNext()) {
				Line previousLine = currentLine;
				currentLine = iterator.next();
				if (Math.abs(currentLine.getTop() - previousLine.getBottom()) > LINE_SPACING_THRESHOLD
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
	public void createBlocks() {
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
			Neighborhood n = new Neighborhood(b);
			while (blockIterator.hasNext()) {
				b = blockIterator.next();
				if (b.verticallyOverlaps(n)) {
					n.add(b);
				} else {
					neighborhoods.add(n);
					n = new Neighborhood(b);
				}
			}
			neighborhoods.add(n);
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

	public String getTableString() {
		return neighborhoods.get(1).getTableString();
	}

	public void printWords() {
		for (Word w : words) {
			System.out.println(w.getText());
			System.out.println(w.getBounds());
		}
	}
}
