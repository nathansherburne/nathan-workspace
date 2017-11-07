package textProcessing;

public class LeftReferencePoint extends ReferencePoint {

	public LeftReferencePoint(MarginPoint mp) {
		super(mp);
	}

	public boolean isLeft() {
		return true;
	}
	
	public boolean isRight() {
		return false;
	}
}
