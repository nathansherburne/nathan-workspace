package textProcessing;

import technology.tabula.TextChunk;
import technology.tabula.TextElement;

@SuppressWarnings("serial")
public class Word extends TextChunk {

	private boolean expanded = false;
	
	public Word(TextElement textElement) {
		super(textElement);
	}
	
	public void setExpanded(boolean b) {
		expanded = b;
	}
	
	public boolean isExpanded() {
		return expanded;
	}
	
	public float getWidthOfSpace() {
		return getLastTextElement().getWidthOfSpace();
	}
	
	private TextElement getLastTextElement() {
		return getTextElements().get(this.getTextElements().size() - 1);
	}
	
}
