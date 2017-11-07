package utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Mat;

public class MyUtils {

	public static BufferedImage toBufferedImage(Mat m) {
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return image;

	}

	public static BufferedImage toBufferedImage2(Mat m) {
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		type = BufferedImage.TYPE_INT_ARGB;
		int bufferSize = m.channels() * m.cols() * m.rows();
		int[] b = new int[bufferSize];
		m.get(0, 0, b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
		int[] data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		// final byte[] targetPixels = ((DataBufferByte)
		// image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, data, 0, b.length);
		return image;

	}

	public static void displayImage(Image im) {
		JFrame frame = new JFrame();
		ImageIcon icon = new ImageIcon(im);
		JLabel label = new JLabel(icon);
		frame.add(label);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	public static BufferedImage toBufferedImageOfType(BufferedImage original, int type) {
		if (original == null) {
			throw new IllegalArgumentException("original == null");
		}

		// Don't convert if it already has correct type
		if (original.getType() == type) {
			return original;
		}

		// Create a buffered image
		BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), type);

		// Draw the image onto the new buffer
		Graphics2D g = image.createGraphics();
		try {
			g.setComposite(AlphaComposite.Src);
			g.drawImage(original, 0, 0, null);
		} finally {
			g.dispose();
		}

		return image;
	}

	public static BufferedImage resize(Image img, int newW, int newH) {
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_3BYTE_BGR);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return dimg;
	}

	/**
	 * Gets a Kelly Color. If the index provided is larger than the number of
	 * Kelly Colors, it will wrap to the beginning.
	 * 
	 * Kelly Colors are useful when one needs a relatively small set of distinct
	 * colors.
	 * 
	 * @param index
	 * @return
	 */
	public static Color getKellyColor(int index) {
		if(index >= KELLY_COLORS.length) {
			index = index % KELLY_COLORS.length;
		}
		return KELLY_COLORS[index];
	}
	
	private static final Color[] KELLY_COLORS = { 
			Color.decode("0xFFB300"), // Vivid Yellow
			Color.decode("0x803E75"), // Strong Purple
			Color.decode("0xFF6800"), // Vivid Orange
			Color.decode("0xA6BDD7"), // Very Light Blue
			Color.decode("0xC10020"), // Vivid Red
			Color.decode("0xCEA262"), // Grayish Yellow
			Color.decode("0x817066"), // Medium Gray
			Color.decode("0x007D34"), // Vivid Green
			Color.decode("0xF6768E"), // Strong Purplish Pink
			Color.decode("0x00538A"), // Strong Blue
			Color.decode("0xFF7A5C"), // Strong Yellowish Pink
			Color.decode("0x53377A"), // Strong Violet
			Color.decode("0xFF8E00"), // Vivid Orange Yellow
			Color.decode("0xB32851"), // Strong Purplish Red
			Color.decode("0xF4C800"), // Vivid Greenish Yellow
			Color.decode("0x7F180D"), // Strong Reddish Brown
			Color.decode("0x93AA00"), // Vivid Yellowish Green
			Color.decode("0x593315"), // Deep Yellowish Brown
			Color.decode("0xF13A13"), // Vivid Reddish Orange
			Color.decode("0x232C16"), // Dark Olive Green
	};

}
