package textProcessing;

public class RightReferencePoint extends ReferencePoint {
	
	public RightReferencePoint(MarginPoint mp) {
		super(mp);
	}

	public boolean isLeft() {
		return false;
	}
	
	public boolean isRight() {
		return true;
	}
}
