package textProcessing;

public class LeftMarginPoint extends MarginPoint {
	public LeftMarginPoint(float x, int height) {
		super(x, height);
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
