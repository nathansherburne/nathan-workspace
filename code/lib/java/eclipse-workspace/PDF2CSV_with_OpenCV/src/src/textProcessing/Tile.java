package textProcessing;

import technology.tabula.Rectangle;
import textProcessing.Tile.TILE_TYPE;

@SuppressWarnings("serial")
public class Tile extends Rectangle {
	private Block block = null;
	private TILE_TYPE type;
	private Tile lowerChild = null;
	private Tile rightChild = null;

	public Tile(float x, float y, float width, float height) {
		super(x, y, width, height);
	}

	public Tile(Rectangle bounds, Block block, TILE_TYPE type) {
		super(bounds.y, bounds.x, bounds.width, bounds.height);
		this.block = block;
		this.type = type;
	}

	public int getRowsSpanned() {
		if(lowerChild == null) {
			return 1;
		}
		return 1 + lowerChild.getRowsSpanned();
	}
	
	public int getColsSpanned() {
		if(rightChild == null) {
			return 1;
		}
		return 1 + rightChild.getColsSpanned();
	}

	public void setType(TILE_TYPE type) {
		this.type = type;
	}

	public void setRightChild(Tile tile) {
		rightChild = tile;
	}

	public void setLowerChild(Tile tile) {
		lowerChild = tile;
	}

	public TILE_TYPE getType() {
		return type;
	}

	public Block getBlock() {
		return block;
	}

	public enum TILE_TYPE {
		TYPE1, TYPE2, TYPE3, TYPE4
	}
}
