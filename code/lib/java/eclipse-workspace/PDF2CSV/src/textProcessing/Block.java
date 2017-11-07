package textProcessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import technology.tabula.HasText;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import technology.tabula.TextChunk;

@SuppressWarnings("serial")
public class Block extends RectangularTextContainer<Word> implements HasText {
	
	protected List<Word> words = new ArrayList<Word>();
	
	public Block(float top, float left, float width, float height) {
		super(top, left, width, height);
	}
	
	public Block(Word word) {
		super(word.y, word.x, word.width, word.height);
		this.add(word);
	}
	
	public List<Word> getWords() {
		return words;
	}
	
	public Block merge(Block other) {
        super.merge(other);
        return this;
    }
	
	public void add(Word word) {
		words.add(word);
		merge(word);
	}
	
	public void add(List<Word> words) {
		for(Word w : words) {
			add(w);
		}
	}
	
	public void removeAll(List<Word> words) {
		for(Word word : words) {
			this.words.remove(word);
		}
		updateDimensions();
	}
	
	public void updateDimensions() {
		if(!words.isEmpty()) {
			Block block = new Block(words.get(0));
			block.add(words);
			setRect(block);
		} else {
			setRect(new Rectangle());
		}	
	}
	
	public float getAvgWidthOfSpace() {
		float avg = 0;
		for(Word w : getWords()) {
			avg += w.getWidthOfSpace();
		}
		return avg / getWords().size();
	}
	
	public float getAvgWordHeight() {
		float avg = 0;
		for(Word w : getWords()) {
			avg += w.getHeight();
		}
		return avg / getWords().size();
	}
	
	public int getNumLines() {
		return (int) (getHeight() / getAvgWordHeight());
	}
	
	public int numWords() {
		return words.size();
	}
	
	public void setAllWordsExpanded(boolean b) {
		for(Word w : words) {
			w.setExpanded(b);
		}
	}
	
	/**
	 * Type 1: if each line in the block has exactly one word (or token).
	 * Type 2: all others
	 * 
	 * TODO: define what a "token" is, so that columns with multi-word entries 
	 * can still be logically defined as Type 1.
	 * 
	 * TODO: store the blocks type in the block and update automatically when words are added.
	 * So that getType() doesn't have to do computation every time.
	 * 
	 * @return
	 */
	public BLOCK_TYPE getType() {
		for(Block line : getLines()) {
			if(line.numWords() > 3) {
				return BLOCK_TYPE.TYPE2;
			}
		}
		return BLOCK_TYPE.TYPE1;
	}
	
	/**
	 * Used for Type 1 blocks, since they are usually columns.
	 * @return
	 */
	public List<? extends Block> decompose() {
		return getLines();
	}
	
	public List<Line> getLines() {
		List<Line> linesOfBlock = new ArrayList<Line>();
		
		Collections.sort(words);
		Iterator<Word> blockIterator = words.iterator();
		if(blockIterator.hasNext()) {
			Word currentWord = blockIterator.next();
			Line currentLine = new Line(currentWord);
			while(blockIterator.hasNext()) {
				currentWord = blockIterator.next();
				if(currentWord.verticallyOverlaps(currentLine)) {
					currentLine.add(currentWord);
				} else {
					linesOfBlock.add(currentLine);
					currentLine = new Line(currentWord);
				}
			}
			linesOfBlock.add(currentLine);
		}
		return linesOfBlock;
	}

	@Override
	public String getText() {
		if(isEmpty()) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		for(TextChunk word : words) {
			sb.append(word.getText());
		}
		
		return sb.toString();
	}

	@Override
	public String getText(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Word> getTextElements() {
		return words;
	}
	
	public boolean isEmpty() {
		return words.isEmpty();
	}
	
	public enum BLOCK_TYPE {
		TYPE1,
		TYPE2
	}

}
