package image_processing;

import org.opencv.core.Mat;

public class ImageInfo {
	private Mat image;
	private Mat binary;
	private Mat horizontalLines;
	private Mat verticalLines;
	private Mat text;
	
	public ImageInfo(Mat image) {
		this.image = image;
	}
	
	public void addBinary(Mat image) {
		binary = image;
	}
	
	public void addHorizontalLines(Mat image) {
		horizontalLines = image;
	}
	
	public void addVerticalLines(Mat image) {
		verticalLines = image;
	}
	
	public void addText(Mat image) {
		text = image;
	}
	
	public Mat getImage() {
		return image;
	}
	
	public Mat getBinary() {
		return binary;
	}
	
	public Mat getHorizontalLines() {
		return horizontalLines;
	}
	
	public Mat getVerticalLines() {
		return verticalLines;
	}
	
	public Mat getText() {
		return text;
	}
	
}
