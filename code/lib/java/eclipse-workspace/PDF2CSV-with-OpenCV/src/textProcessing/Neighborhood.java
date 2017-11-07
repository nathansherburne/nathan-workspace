package textProcessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.TreeSet;

import technology.tabula.HasText;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import textProcessing.Block.BLOCK_TYPE;
import textProcessing.Tile.TILE_TYPE;

@SuppressWarnings("serial")
public class Neighborhood extends RectangularTextContainer<Block> implements HasText {
	private List<Block> blocks = new ArrayList<Block>();
	private MarginStructure marginStructure = null;

	public Neighborhood(float top, float left, float width, float height) {
		super(top, left, width, height);
	}

	public Neighborhood(Block block) {
		super(block.y, block.x, block.width, block.height);
		this.add(block);
	}

	public MarginStructure getMarginStructure() {
		if (marginStructure == null) {
			setMarginStructure();
		}
		return marginStructure;
	}

	private MarginStructure getMarginStructure(List<Block> blocks) {
		MarginStructure marg = new MarginStructure();
		for (Block b : blocks) {
			marg.addBlock(b);
		}
		return marg;
	}
	
	public void setMarginStructure() {
		marginStructure = getMarginStructure(blocks);
	}

	public void add(Block block) {
		blocks.add(block);
		this.merge(block);
	}

	public void mergeIsolated() {
		MarginStructure marginStructure = getMarginStructure();
		final int MIN_HEIGHT = 3;
		// get margin points
		// get reference points
		for (ReferencePoint rp : marginStructure.getReferencePoints()) {
			// See MarginPoint comments that explain why reference point height must
			// be calculated on the fly, and not stored in the referencePoint object.
			int refHeight = 0;
			for(Block b : marginStructure.getBlocks(rp.getMPs())) {
				refHeight += b.getNumLines();
			}
			if (refHeight < MIN_HEIGHT) {
				if (rp.isLeft()) {
					// Potentially merge blocks in this RP with neighbors if in range.
					for (Block b : marginStructure.getBlocks(rp.getMPs())) {
						potentiallyMergeBlockLeft(b);
					}
				}
				// if (rp.isRight()) {
				// for (Block b : marginStructure.getBlocks(rp.getMPs())) {
				// potentiallyMergeBlockRight(b);
				// }
				// }
			}
		}
		// find reference points L-R within x-range
		// potentially merge their blocks
	}

	/**
	 * Looks for a block close enough on the left to merge with. If found, merge.
	 * 
	 * @param b
	 * @return true if merged. false otherwise.
	 */
	private boolean potentiallyMergeBlockLeft(Block b) {
		Block neighbor = null;
		float closest = java.lang.Float.MAX_VALUE;
		for (Block potentialNeighbor : blocks) {
			if (potentialNeighbor.equals(b)) {
				continue;
			}
			if (potentialNeighbor.verticallyOverlaps(b)) {
				float distance = Math.abs(horizontalOverlapValue(b, potentialNeighbor));
				if (distance < closest) {
					closest = distance;
					neighbor = potentialNeighbor;
				}
			}
		}
		if (neighbor == null)
			return false;
		float spacing = horizontalOverlapValue(b, neighbor);
		if (spacing >= mergeThreshold(b)) {
			blocks.remove(b);
			blocks.remove(neighbor);
			blocks.add(neighbor.merge(b));
			return true;
		}
		return false;
	}

	private boolean potentiallyMergeBlockRight(Block b) {
		Block neighbor = null;
		float closest = java.lang.Float.MAX_VALUE;
		for (Block potentialNeighbor : blocks) {
			if (b.equals(potentialNeighbor)) {
				continue;
			}
			if (potentialNeighbor.horizontallyOverlaps(b)) {
				float distance = Math.abs(horizontalOverlapValue(b, potentialNeighbor));
				if (distance < closest) {
					closest = distance;
					neighbor = potentialNeighbor;
				}
			}
		}
		if (neighbor == null)
			return false;
		float spacing = horizontalOverlapValue(b, neighbor);
		if (spacing >= mergeThreshold(b)) {
			blocks.remove(b);
			blocks.remove(neighbor);
			blocks.add(neighbor.merge(b));
			return true;
		}
		return false;
	}

	public float mergeThreshold(Block b) {
		int numberOfSpaces = 3;
		return b.getAvgWidthOfSpace() * numberOfSpaces * -1;
	}

	public float horizontalOverlapValue(Rectangle a, Rectangle b) {
		return Math.min(a.getRight(), b.getRight()) - Math.max(a.getLeft(), b.getLeft());
	}

	public void decomposeType1Blocks() {
		setMarginStructure();
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

	public Tile[][] getTiles() {
		Collections.sort(blocks);
		TreeSet<java.lang.Float> horizontalRulings = getHorizontalRulings();
		TreeSet<java.lang.Float> verticalRulings = getVerticalRulings();
		if(horizontalRulings.isEmpty() || verticalRulings.isEmpty()) {
			return null;
		}
		Table tileStructure = new Table(horizontalRulings, verticalRulings);
		return tileStructure.generateTiles(blocks);
	}

	public String getTableString() {
		StringBuilder sb = new StringBuilder();
		Tile[][] tiles = getTiles();
		if(tiles == null) {
			System.err.println("Error (Neighborhood.getTableString()): Tiles could not be generated.");
			System.exit(1);
		}
		sb.append("<TABLE>");
		sb.append(System.getProperty("line.separator"));
		for (int i = 0; i < tiles.length; i++) {
			sb.append("<TR>");
			for (int j = 0; j < tiles[i].length; j++) {
				Tile curTile = tiles[i][j];
				if (curTile.getType() == TILE_TYPE.TYPE1) {
					sb.append("<TD></TD>");
					sb.append(System.getProperty("line.separator"));
				} else if (curTile.getType() == TILE_TYPE.TYPE2) {
					sb.append("<TD>");
				} else if (curTile.getType() == TILE_TYPE.TYPE3) {
					sb.append("<TD COLSPAN=" + curTile.getColsSpanned() + " ROWSPAN=" + curTile.getRowsSpanned() + ">");
				} else if (curTile.getType() == TILE_TYPE.TYPE4) {
					// Do nothing
				}
				if (curTile.getType() == TILE_TYPE.TYPE2 || curTile.getType() == TILE_TYPE.TYPE3) {
					String tileTextContent = curTile.getBlock().getText();
					sb.append(tileTextContent);
					sb.append("</TD>");
					sb.append(System.getProperty("line.separator"));
				}
				if (j + 1 == tiles[i].length) {
					sb.append("</TR>");
					sb.append(System.getProperty("line.separator"));
				}
			}
		}
		sb.append("</TABLE>");
		sb.append(System.getProperty("line.separator"));
		return sb.toString();
	}

	public TreeSet<java.lang.Float> getHorizontalRulings() {
		TreeSet<java.lang.Float> rows = new TreeSet<java.lang.Float>();
		List<Block> blocks = getType1Blocks();
		Iterator<Block> iter = blocks.iterator();
		if (iter.hasNext()) {
			Block curBlock = iter.next();
			Block prevBlock;
			float curRowMinY = curBlock.getTop();
			float maxY = curBlock.getBottom();
			while (iter.hasNext()) {
				prevBlock = curBlock;
				curBlock = iter.next();
				if (curBlock.verticallyOverlaps(prevBlock)) {
					curRowMinY = curBlock.getTop() < prevBlock.getTop() ? curBlock.getTop() : prevBlock.getTop();
				} else {
					rows.add(curRowMinY);
					curRowMinY = curBlock.getTop();
				}
				maxY = curBlock.getBottom() > prevBlock.getBottom() ? curBlock.getBottom() : prevBlock.getBottom();
			}
			rows.add(curRowMinY);
			rows.add(maxY); // Add the bottom line.
		}
		return rows;

	}

	public TreeSet<java.lang.Float> getVerticalRulings() {
		MarginStructure marg = getMarginStructure();
		List<ReferencePoint> RPs = marg.getReferencePoints();
		TreeSet<java.lang.Float> columns = new TreeSet<java.lang.Float>();
		boolean wasRight = false;
		float prevRightX = 0;
		float minX = java.lang.Float.MAX_VALUE;
		float maxX = 0;
		ListIterator<ReferencePoint> iter = RPs.listIterator();
		while (iter.hasNext()) {
			ReferencePoint rp = iter.next();
			if (rp.isRight()) {
				wasRight = true;
				prevRightX = rp.getRight();
			} else if (rp.isLeft()) {
				if (wasRight) {
					// Change from right to left means column.
					float left = rp.getLeft();
					columns.add((prevRightX + left) / 2);
					wasRight = false;
				}
			}
			maxX = rp.getRight() > maxX ? rp.getRight() : maxX;
			minX = rp.getLeft() < minX ? rp.getLeft() : minX;
		}
		columns.add(maxX);
		columns.add(minX);
		return columns;
	}

	public List<Block> getType1Blocks() {
		List<Block> type1Blocks = new ArrayList<Block>();
		for (Block b : blocks) {
			if (b.getType() == BLOCK_TYPE.TYPE1) {
				type1Blocks.add(b);
			}
		}
		return type1Blocks;
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
		return blocks;
	}

}
