package textProcessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class MarginStructure {
	TreeMap<RightMarginPoint, ArrayList<Block>> rightMarginPoints = new TreeMap<RightMarginPoint, ArrayList<Block>>();
	TreeMap<LeftMarginPoint, ArrayList<Block>> leftMarginPoints = new TreeMap<LeftMarginPoint, ArrayList<Block>>();
	private double spaceScale;
	public MarginStructure(double spaceScale) {
		this.spaceScale = spaceScale;
	}

	public TreeMap<RightMarginPoint, ArrayList<Block>> getRightMPs() {
		return rightMarginPoints;
	}

	public TreeMap<LeftMarginPoint, ArrayList<Block>> getLeftMPs() {
		return leftMarginPoints;
	}

	public void addBlock(Block block) {
		// Margin point height, and thus reference pointer height, must be calculated
		// on the fly. Margin points do not know their own height since they are simply
		// the keys that map to a list of blocks. Because of the way maps work in Java,
		// a key cannot be fetched and updated when its value is updated.
		LeftMarginPoint lmp = new LeftMarginPoint(block.getLeft(), block.getNumLines());
		if (!leftMarginPoints.containsKey(lmp)) {
			leftMarginPoints.put(lmp, new ArrayList<Block>());
		}
		leftMarginPoints.get(lmp).add(block);

		RightMarginPoint rmp = new RightMarginPoint(block.getRight(), block.getNumLines());
		if (!rightMarginPoints.containsKey(rmp)) {
			rightMarginPoints.put(rmp, new ArrayList<Block>());
		}
		rightMarginPoints.get(rmp).add(block);
	}

	public int getRefCount(MarginPoint mp) {
		if (mp.isLeft()) {
			return leftMarginPoints.get(mp).size();
		}
		return rightMarginPoints.get(mp).size();
	}

	public ArrayList<Block> getBlocks(MarginPoint mp) {
		if (mp.isLeft()) {
			return leftMarginPoints.get(mp);
		}
		return rightMarginPoints.get(mp);

	}

	public ArrayList<Block> getBlocks(List<MarginPoint> mps) {
		ArrayList<Block> referencedBlocks = new ArrayList<Block>();
		for (MarginPoint mp : mps) {
			referencedBlocks.addAll(getBlocks(mp));
		}
		return referencedBlocks;
	}

	public double maxBlockWidth(MarginPoint mp) {
		double maxWidth = 0;
		for (Block block : getBlocks(mp)) {
			if (block.getWidth() > maxWidth) {
				maxWidth = block.getWidth();
			}
		}
		return maxWidth;
	}

	public double avgWidthOfSpace(MarginPoint mp) {
		double avgWidthOfSpace = 0;
		ArrayList<Block> blocks = getBlocks(mp);
		for (Block block : blocks) {
			avgWidthOfSpace += block.getAvgWidthOfSpace();
		}
		return avgWidthOfSpace / blocks.size();
	}

	/**
	 * RP Threshold is defined as the width of two spaces.
	 * @param mp
	 * @return
	 */
	public double getRPThreshold(MarginPoint mp) {
		return avgWidthOfSpace(mp) * 2 * spaceScale;
	}

	/**
	 * Constructs reference points based on the margin points. A reference point is
	 * an accumulation of margin points of the same type that occur within a
	 * threshold x-range. A reference point accumulates the height of its margin
	 * points.
	 * 
	 * @return
	 */
	public List<ReferencePoint> getReferencePoints() {
		List<ReferencePoint> RPs = new ArrayList<ReferencePoint>();
		LeftMarginPoint prevLeft, curLeft;
		LeftReferencePoint leftRP;

		Iterator<LeftMarginPoint> LMPIterator = leftMarginPoints.keySet().iterator();
		if (LMPIterator.hasNext()) {
			curLeft = LMPIterator.next();
			leftRP = new LeftReferencePoint(curLeft);
			while (LMPIterator.hasNext()) {
				prevLeft = curLeft;
				curLeft = LMPIterator.next();
				if (curLeft.getX() - prevLeft.getX() <= getRPThreshold(prevLeft)) {
					leftRP.add(curLeft);
				} else {
					RPs.add(leftRP);
					leftRP = new LeftReferencePoint(curLeft);
				}
			}
			RPs.add(leftRP);
		}

		RightMarginPoint prevRight, curRight;
		RightReferencePoint rightRP;

		Iterator<RightMarginPoint> RMPIterator = getRightMPs().keySet().iterator();
		if (RMPIterator.hasNext()) {
			curRight = RMPIterator.next();
			rightRP = new RightReferencePoint(curRight);
			while (RMPIterator.hasNext()) {
				prevRight = curRight;
				curRight = RMPIterator.next();
				if (curRight.getX() - prevRight.getX() <= getRPThreshold(prevRight)) {
					rightRP.add(curRight);
				} else {
					RPs.add(rightRP);
					rightRP = new RightReferencePoint(curRight);
				}
			}
			RPs.add(rightRP);
		}
		Collections.sort(RPs);
		return RPs;
	}
}
