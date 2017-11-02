package driver;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.util.Matrix;
import org.opencv.core.Core;

import technology.tabula.Rectangle;
import technology.tabula.TextStripper;
import technology.tabula.Utils;
import textProcessing.Document;
import textProcessing.Neighborhood;
import utils.MyUtils;

public class Driver {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) throws InvalidPasswordException, IOException {
		//// Command Line Options ////
		Options options = new Options();

		Option input = Option.builder("i")
				.argName("input")
				.hasArg()
				.desc("The input PDF file(s)")
				.build();
		options.addOption(input);
		
		Option output = new Option("o", "output", true, "output file (HTML)");
		output.setRequired(true);
		options.addOption(output);

		Option pageOpt = new Option("p", "page", true, "the page number");
		pageOpt.setRequired(true);
		options.addOption(pageOpt);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);

			System.exit(1);
			return;
		}

		String[] inputFiles = cmd.getOptionValues("i");
		String outputDirectory = cmd.getOptionValue("output");
		int pageNum = Integer.valueOf(cmd.getOptionValue("page")) - 1;
		
		for(int i = 0; i < inputFiles.length; i++) {
			String outPath = getOutputPath(inputFiles[i], outputDirectory, ".html");
			PDF2HTML(new File(inputFiles[i]), pageNum, outPath);
		}
		
	}
	
	/**
	 * Generates the String for the output path using the input path/filename as the basename
	 * for the output, just with a different directory path and a different extension.
	 * @param inputPath
	 * @param outputDirectory
	 * @return
	 */
	public static String getOutputPath(String inputPath, String outputDirectory, String ext) {
		String[] tokens = inputPath.split(".+?/(?=[^/]+$)");
		String filename = tokens[1];
		tokens = filename.split("\\.(?=[^\\.]+$)");
		String basename = tokens[0];
		String outputPath = outputDirectory + '/' + basename + ext;
		return outputPath;
	}

	public static void PDF2HTML(File pdfFile, int pageNum, String outputFilePath)
			throws InvalidPasswordException, IOException {
		
		//// Start ////
		PDDocument pdfDocument = PDDocument.load(pdfFile);
		
		Document document = new Document(pdfDocument, 1);
		
		document.createWords();
		document.createLines();
		document.createDummyLines();
		document.createBlocks();
		document.isolateMergedColumns();
		document.createNeighborhoods();
		document.mergeIsolateBlocks();
		document.decomposeType1Blocks();
		drawing(pdfDocument, pageNum, document);
//
//		PrintWriter writer = new PrintWriter(outputFilePath, "UTF-8");
//		writer.print(document.getTableString());
//		writer.close();

		// PDFRenderer renderer = new PDFRenderer(pdfDocument);
		pdfDocument.close();

		// TableFinder -> rulings
		// -> TableExtractor
		// -> CSVWriter
	}

	/// DRAWING ///
	public static void drawing(PDDocument pdfDocument, int pageNum, Document document) throws IOException {
		// drawBlocks(pdfDocument, pageNum, document.getBlocks(), Color.RED);
		int c = 0;
		PDPage page = pdfDocument.getPage(pageNum);
		Matrix rotationMatrix = getRotationMatrix(page.getRotation(), page.getCropBox().getWidth(),
				page.getCropBox().getHeight());
		if (rotationMatrix == null) {
			System.err.println("Unsupported rotation angle (i.e. not 0, 90, 180, or 270.");
			return;
		}
		PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page,
				PDPageContentStream.AppendMode.APPEND, true);
		contentStream.transform(rotationMatrix);

		contentStream.setLineWidth(1.0f);
		for (Neighborhood n : document.getNeighborhoods()) {
			// Tile[][] tiles = n.getTiles();
			// List<Tile> tiles2 = new ArrayList<Tile>();
			// for (int i = 0; i < tiles.length; i++) {
			// for (int j = 0; j < tiles[i].length; j++) {
			// tiles2.add(tiles[i][j]);
			// }
			// }
			// List<Rectangle> ns = new ArrayList<Rectangle>();
			// ns.add(n);
			// ns.addAll(tiles2);
			drawBlocks(contentStream, n.getTextElements(), MyUtils.KELLY_COLORS[c++]);
		}
		contentStream.close();
		// drawBlocks(pdfDocument, 0, document.getBlocks(),
		// "src/tests/resources/draw2.pdf", Color.RED);
		pdfDocument.save("src/tests/resources/blockDrawing.pdf");

	}

	public static Matrix getRotationMatrix(int rotation, float width, float height) throws IOException {
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

	public static void drawBlocks(PDPageContentStream contentStream, List<? extends Rectangle> tiles, Color c)
			throws IOException {
		contentStream.setStrokingColor(c);
		for (Rectangle b : tiles) {
			contentStream.addRect(b.x, b.y, b.width, b.height);
		}
		contentStream.stroke();
	}

	private static void drawLine(PDPageContentStream content, float xstart, float ystart, float xend, float yend)
			throws IOException {
		content.moveTo(xstart, ystart); // moves "pencil" to a position
		content.lineTo(xend, yend); // creates an invisible line to another position
		content.stroke(); // makes the line visible
	}
	
	public static void displayImage(BufferedImage img) {
		JFrame frame = new JFrame();
		  ImageIcon icon = new ImageIcon(img);
		  JLabel label = new JLabel(icon);
		  frame.add(label);
		  frame.setDefaultCloseOperation
		         (JFrame.EXIT_ON_CLOSE);
		  frame.pack();
		  frame.setVisible(true);
	}
}
