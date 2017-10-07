package textProcessing;

public class RightMarginPoint extends MarginPoint {
	public RightMarginPoint(float x) {
		super(x);
	}
	
	@Override
	public float getX() {
		return block.getRight();
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
