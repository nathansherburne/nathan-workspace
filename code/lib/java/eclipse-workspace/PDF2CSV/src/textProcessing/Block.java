package textProcessing;

import java.util.ArrayList;
import java.util.List;

import technology.tabula.HasText;
import technology.tabula.RectangularTextContainer;
import technology.tabula.TextChunk;

@SuppressWarnings("serial")
public class Block extends RectangularTextContainer<Word> implements HasText {
	
	private List<Word> words = new ArrayList<Word>();
	
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
	
	public float getAvgWidthOfSpace() {
		float avg = 0;
		for(Word w : getWords()) {
			avg += w.getWidthOfSpace();
		}
		return avg / getWords().size();
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

}
