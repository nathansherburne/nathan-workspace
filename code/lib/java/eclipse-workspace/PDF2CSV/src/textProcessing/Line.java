package textProcessing;

import java.util.ArrayList;
import java.util.List;

import technology.tabula.HasText;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import technology.tabula.TextChunk;
import technology.tabula.TextElement;


@SuppressWarnings("serial")
public class Line extends RectangularTextContainer<Word> implements HasText {
	List<Word> words = new ArrayList<Word>();
	
	public Line(float top, float left, float width, float height) {
		super(top, left, width, height);
	}
	
	public Line(Word word) {
		super(word.y, word.x, word.width, word.height);
		this.add(word);
	}
	
	/**
	 * Adds a text chunk to the line only if it overlaps it.
	 * @param t
	 * @return
	 */
	public boolean add(Word t) {
		if(!t.verticallyOverlaps(this)) {
			return false;
		}
		words.add(t);
		this.merge(t);
		return true;
	}
	
	public List<Word> getWords() {
		return words;
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
