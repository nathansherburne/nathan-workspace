package textProcessing;

public class RightMarginPoint extends MarginPoint {
	public RightMarginPoint(Block block) {
		super(block);
	}
	
	@Override
	public float getX() {
		return blocksWithThisMP.get(0).getRight();
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
