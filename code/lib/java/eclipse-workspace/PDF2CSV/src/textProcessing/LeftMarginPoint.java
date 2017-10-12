package textProcessing;

public class LeftMarginPoint extends MarginPoint {
	public LeftMarginPoint(Block block) {
		super(block);
	}
	
	@Override
	public float getX() {
		return blocksWithThisMP.get(0).getLeft();
	}

	@Override
	public boolean isLeft() {
		return true;
	}

	@Override
	public boolean isRight() {
		return false;
	}
}
