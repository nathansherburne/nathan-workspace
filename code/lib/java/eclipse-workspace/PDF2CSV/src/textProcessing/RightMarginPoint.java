package textProcessing;

public class RightMarginPoint extends MarginPoint {
	public RightMarginPoint(float x, int height) {
		super(x, height);
	}

	@Override
	public boolean isLeft() {
		return false;
	}

	@Override
	public boolean isRight() {
		return true;
	}
}
