package textProcessing;

public class LeftMarginPoint extends MarginPoint {
	public LeftMarginPoint(float x) {
		super(x);
	}
	
	@Override
	public float getX() {
		return block.getLeft();
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
