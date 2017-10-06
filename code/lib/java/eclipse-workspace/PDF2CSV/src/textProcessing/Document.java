package textProcessing;

import java.util.ArrayList;
import java.util.List;

public class Document {
	List<Line> lines = new ArrayList<Line>();
	List<Block> blocks = new ArrayList<Block>();
	List<Word> words = new ArrayList<Word>();

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

	public List<Block> getBlocks() {
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

		if (currentLineNumber - 1 >= 0) {  // Has previous line
			ovlAbove = getOvlWords(seed, getLines().get(currentLineNumber - 1));
		}
		if (currentLineNumber + 1 < getLines().size()) {  // Has next line
			ovlBelow = getOvlWords(seed, getLines().get(currentLineNumber + 1));
		}
		
		for(Word ovlWord : ovlAbove) {
			if(ovlWord.isExpanded()) continue;
			block.merge(createBlock(ovlWord, currentLineNumber - 1));
		}
		for(Word ovlWord : ovlBelow) {
			if(ovlWord.isExpanded()) continue;
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
}
