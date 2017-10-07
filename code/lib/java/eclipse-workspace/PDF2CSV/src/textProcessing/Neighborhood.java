package textProcessing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import technology.tabula.HasText;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;

@SuppressWarnings("serial")
public class Neighborhood extends RectangularTextContainer<Block> implements HasText {

	private List<Block> blocks = new ArrayList<Block>();
	private MarginStructure marginStructure;

	public Neighborhood(float top, float left, float width, float height) {
		super(top, left, width, height);
		marginStructure = new MarginStructure();
		marginStructure.addMarginPoints(this);
	}

	public Neighborhood(Block block) {
		super(block.y, block.x, block.width, block.height);
		this.add(block);
		marginStructure = new MarginStructure(block.getAvgWidthOfSpace());
		marginStructure.addMarginPoints(block);
	}
	
	public MarginStructure getMarginStructure() {
		return marginStructure;
	}

	public void add(Block block) {
		blocks.add(block);
		marginStructure.addMarginPoints(block);
	}

	@Override
	public String getText() {
		if (isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Block block : blocks) {
			sb.append(block.getText());
		}

		return sb.toString();
	}

	@Override
	public String getText(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Block> getTextElements() {
		return blocks;
	}

}
