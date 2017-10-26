package textProcessing;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import technology.tabula.HasText;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import textProcessing.Tile.TILE_TYPE;

@SuppressWarnings("serial")
public class Table extends RectangularTextContainer<Block> implements HasText {

	private TreeSet<java.lang.Float> horizontalRulings;
	private TreeSet<java.lang.Float> verticalRulings;

	public Table(TreeSet<java.lang.Float> horizontalRulings, TreeSet<java.lang.Float> verticalRulings) {
		super(horizontalRulings.first(), verticalRulings.first(),  // top and left
				verticalRulings.last() - verticalRulings.first(),  // Width
				horizontalRulings.last() - horizontalRulings.first()); // Height
		this.horizontalRulings = horizontalRulings;
		this.verticalRulings = verticalRulings;
	}

	public Tile[][] generateTiles(List<Block> blocks) {
		List<Rectangle2D.Float> rows = getRows(horizontalRulings, getLeft(), getRight());
		List<Rectangle2D.Float> cols = getCols(verticalRulings, getTop(), getBottom());

		Iterator<Block> blockIter = blocks.iterator();
		Rectangle curTile;
		Tile[][] tiles = new Tile[rows.size()][cols.size()];
		Block curBlock = null;
		boolean curBlockIsProcessed = true;
		for (int i = 0; i < rows.size(); i++) {
			for (int j = 0; j < cols.size(); j++) {
				Rectangle2D rect = rows.get(i).createIntersection(cols.get(j));
				curTile = new Rectangle((float) rect.getY(), (float) rect.getX(), (float) rect.getWidth(),
						(float) rect.getHeight());
				if (curBlockIsProcessed) {
					if (blockIter.hasNext()) {
						curBlock = blockIter.next();
					} else {
						curBlock = null;
					}
				}
				if (curBlock == null) {
					// Just a blank cell
					tiles[i][j] = new Tile(curTile, curBlock, TILE_TYPE.TYPE1);
				} else if (curBlock.horizontallyOverlaps(curTile) && curBlock.verticallyOverlaps(curTile)) {
					// There is a new block.
					tiles[i][j] = new Tile(curTile, curBlock, TILE_TYPE.TYPE2);
					curBlockIsProcessed = true;
				} else {
					// There is not a new block, but check for overlapping blocks right and up.
					curBlockIsProcessed = false;
					// Dummy tiles for when we are on the top or left edge.
					Tile up = new Tile(0, 0, 0, 0);
					Tile left = new Tile(0, 0, 0, 0);
					if (i > 0) {
						up = tiles[i - 1][j];
					}
					if (j > 0) {
						left = tiles[i][j - 1];
					}
					if (curBlock.horizontallyOverlaps(up) && curBlock.verticallyOverlaps(up)) {
						tiles[i][j] = new Tile(curTile, up.getBlock(), TILE_TYPE.TYPE4);
						up.setType(TILE_TYPE.TYPE3);
						up.setLowerChild(tiles[i][j]);
					} else if (curBlock.horizontallyOverlaps(left) && curBlock.verticallyOverlaps(left)) {
						tiles[i][j] = new Tile(curTile, left.getBlock(), TILE_TYPE.TYPE4);
						left.setType(TILE_TYPE.TYPE3);
						left.setRightChild(tiles[i][j]);
					} else {
						// Just a blank cell
						tiles[i][j] = new Tile(curTile, curBlock, TILE_TYPE.TYPE1);
					}
				}
			}
		}
		return tiles;
	}

	public List<Rectangle2D.Float> getRows(TreeSet<java.lang.Float> horizontalRulings, float left, float right) {
		List<Rectangle2D.Float> rows = new ArrayList<Rectangle2D.Float>();
		float x, y, w, h;
		Iterator<java.lang.Float> iter = horizontalRulings.iterator();
		if (iter.hasNext()) {
			float botRul = iter.next();
			float topRul;
			x = left;
			w = right - left;
			while (iter.hasNext()) {
				topRul = botRul;
				botRul = iter.next();
				y = topRul;
				h = botRul - topRul;
				rows.add(new Rectangle2D.Float(x, y, w, h));
			}
		}
		return rows;
	}

	public List<Rectangle2D.Float> getCols(TreeSet<java.lang.Float> verticalRulings, float top, float bottom) {
		List<Rectangle2D.Float> cols = new ArrayList<Rectangle2D.Float>();
		float x, y, w, h;
		Iterator<java.lang.Float> iter = verticalRulings.iterator();
		if (iter.hasNext()) {
			float rightRul = iter.next();
			float leftRul;
			y = top;
			h = bottom - top;
			while (iter.hasNext()) {
				leftRul = rightRul;
				rightRul = iter.next();
				x = leftRul;
				w = rightRul - leftRul;
				cols.add(new Rectangle2D.Float(x, y, w, h));
			}
		}
		return cols;
	}

	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getText(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Block> getTextElements() {
		// TODO Auto-generated method stub
		return null;
	}
}
