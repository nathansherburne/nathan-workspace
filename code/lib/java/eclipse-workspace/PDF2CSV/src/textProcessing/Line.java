package textProcessing;

import java.util.List;

import technology.tabula.TextChunk;


@SuppressWarnings("serial")
public class Line extends Block {
	
	public Line(float top, float left, float width, float height) {
		super(top, left, width, height);
	}
	
	public Line(Word word) {
		super(word.y, word.x, word.width, word.height);
		this.add(word);
	}
	
	/**
	 * Adds a word to the line only if it overlaps it.
	 * @param t
	 * @return
	 */
	@Override
	public void add(Word t) {
		if(!t.verticallyOverlaps(this)) {
			return;
		}
		super.add(t);
		this.merge(t);
		return;
	}

	@Override
	public List<Word> getTextElements() {
		return words;
	}
	
	public boolean isEmpty() {
		return words.isEmpty();
	}


}
