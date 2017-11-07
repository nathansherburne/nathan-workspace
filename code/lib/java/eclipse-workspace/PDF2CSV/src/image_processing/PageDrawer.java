package image_processing;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.util.Matrix;

import technology.tabula.Rectangle;
import textProcessing.Document;

public class PageDrawer {

	private Document document;
	private PDPage page;
	private Matrix rotationMatrix = new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, 1, 0, 0)); // Identity matrix
	private PDPageContentStream contentStream;

	public PageDrawer(Document document) {
		this.document = document;
		init();
	}

	public void init() {
		this.page = document.getPDDocument().getPage(document.getPageNum());
		try {
			this.rotationMatrix = getRotationMatrix(page.getRotation(), page.getCropBox().getWidth(),
					page.getCropBox().getHeight());
			if (rotationMatrix == null) {
				System.err.println("Unsupported rotation angle (i.e. not 0, 90, 180, or 270.");
				
			}
			contentStream = new PDPageContentStream(document.getPDDocument(), page,
					PDPageContentStream.AppendMode.APPEND, true);
			contentStream.transform(rotationMatrix);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void drawWords() {
		List<? extends Rectangle> words = document.getWords();
		draw(words);
	}

	public void drawLines() {
		List<? extends Rectangle> lines = document.getLines();
		draw(lines);
	}

	public void drawBlocks() {
		List<? extends Rectangle> blocks = document.getBlocks();
		draw(blocks);
	}

	public void draw(List<? extends Rectangle> rectangles) {
		try {
			contentStream = new PDPageContentStream(document.getPDDocument(), page,
					PDPageContentStream.AppendMode.APPEND, true);
			contentStream.transform(rotationMatrix);
			drawBlocks(contentStream, rectangles, Color.RED);
			contentStream.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public Matrix getRotationMatrix(int rotation, float width, float height) throws IOException {
		if (rotation == 0) {
			// return new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, 1, 0, 0)); //
			// Identity matrix
			return new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, -1, 0, height)); // Vertical mirror
		}
		if (rotation == 90) {
			return new Matrix(new java.awt.geom.AffineTransform(0, 1, 1, 0, 0, 0)); // Rotate counter-clockwise
		}
		// 180
		// 270
		return null; //

	}

	public void drawBlocks(PDPageContentStream contentStream, List<? extends Rectangle> tiles, Color c)
			throws IOException {
		contentStream.setStrokingColor(c);
		for (Rectangle b : tiles) {
			contentStream.addRect(b.x, b.y, b.width, b.height);
		}
		contentStream.stroke();
	}

	public void displayImage(BufferedImage img) {
		JFrame frame = new JFrame();
		ImageIcon icon = new ImageIcon(img);
		JLabel label = new JLabel(icon);
		frame.add(label);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
