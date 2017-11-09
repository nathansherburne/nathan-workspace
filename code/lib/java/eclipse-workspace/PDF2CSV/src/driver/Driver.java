package driver;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

import technology.tabula.Rectangle;
import textProcessing.Block;
import textProcessing.Document;
import textProcessing.Neighborhood;
import textProcessing.Tile;
import utils.MyUtils;

public class Driver {

	public static void main(String[] args) throws InvalidPasswordException, IOException {
		//// Command Line Options ////
		Options options = new Options();

		Option input = Option.builder("i").argName("input").hasArg().desc("The input PDF file(s)")
				.numberOfArgs(Option.UNLIMITED_VALUES).build();
		options.addOption(input);

		Option output = new Option("o", "output", true, "output file (HTML)");
		output.setRequired(true);
		options.addOption(output);

		Option pageOpt = new Option("p", "page", true, "the page number");
		pageOpt.setRequired(true);
		options.addOption(pageOpt);

		Option debugOpt = new Option("d", "debug", false, "save debug PDFs / print debug info");
		options.addOption(debugOpt);

		Option minRowsOpt = new Option("m", "min-rows", true,
				"will not be considered a table if it doesn't have this many rows");
		options.addOption(minRowsOpt);

		Option lineSpacingThresholdOpt = new Option("l", "line-spacing", true,
				"A threshold value for creating dummy lines. A higher threshold means less dummy lines will be inserted");
		options.addOption(lineSpacingThresholdOpt);

		Option spaceScaleOpt = new Option("s", "space-scale", true,
				"Used to scale the definition of width of space. A value < 1 will decrease the width of space, "
						+ "resulting in more words, since a small space in between letters will be considered a 'space' character");
		options.addOption(spaceScaleOpt);

		Option xOpt = new Option("x", "x-coord", true, "x coordinate of top-left of ROI");
		options.addOption(xOpt);

		Option yOpt = new Option("y", "y-coord", true, "y coordinate of top-left of ROI");
		options.addOption(yOpt);

		Option wOpt = new Option("w", "width", true, "width of ROI");
		options.addOption(wOpt);

		Option hOpt = new Option("h", "height", true, "height of ROI");
		options.addOption(hOpt);

		Option testROIOpt = new Option("t", "roi-test", false, "draw the specified ROI on the PDF, save it, and quit.");
		options.addOption(testROIOpt);

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

		// Get command line options into variables
		String[] inputFiles = cmd.getOptionValues("i");
		String outputDirectory = cmd.getOptionValue("output");
		boolean debug = cmd.hasOption("debug");
		Integer pageNum = null;
		Integer minRows = null;
		Double lineSpacingThreshold = null; 
		Double spaceScale = null;
		Rectangle2D.Float roi = null;

		if(cmd.hasOption("page")) {
			pageNum = Integer.valueOf(cmd.getOptionValue("page")) - 1;
		}
		if(cmd.hasOption("min-rows")) {
			minRows = Integer.valueOf(cmd.getOptionValue("min-rows"));
		}
		if (cmd.hasOption("line-spacing")) {
			lineSpacingThreshold = Double.valueOf(cmd.getOptionValue("line-spacing"));
		}
		if (cmd.hasOption("space-scale")) {
			spaceScale = Double.valueOf(cmd.getOptionValue("space-scale"));
		}
		if (cmd.hasOption("x-coord") && cmd.hasOption("y-coord") && cmd.hasOption("width") && cmd.hasOption("height")) {
			float x = Float.valueOf(cmd.getOptionValue("x-coord"));
			float y = Float.valueOf(cmd.getOptionValue("y-coord"));
			float w = Float.valueOf(cmd.getOptionValue("width"));
			float h = Float.valueOf(cmd.getOptionValue("height"));
			roi = new Rectangle2D.Float(x, y, w, h);
		} else {
			if (cmd.hasOption("roi-test")) {
				throw new IOException("invalid options: must supply roi(x, y, w, h) for roi-test");
			}
		}
		
		
		if (cmd.hasOption("roi-test")) {
			for (int i = 0; i < inputFiles.length; i++) {
				String outPath = getOutputPath(inputFiles[i], outputDirectory, "_ROI.pdf");
				drawROI(new File(inputFiles[i]), pageNum, outPath, roi);
			}
		} else {
			for (int i = 0; i < inputFiles.length; i++) {
				String outPath = getOutputPath(inputFiles[i], outputDirectory, ".html");
				PDF2HTML(new File(inputFiles[i]), pageNum, outPath, debug, outputDirectory, minRows,
						lineSpacingThreshold, spaceScale, roi);
			}
		}

	}

	/**
	 * Generates the String for the output path using the input path/filename as the
	 * basename for the output, just with a different directory path and a different
	 * extension.
	 * 
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

	public static void PDF2HTML(File pdfFile, Integer pageNum, String outputFilePath, boolean debug, String debugDir,
			Integer minRows, Double lineSpacingThreshold, Double spaceScale, Rectangle2D roi)
			throws InvalidPasswordException, IOException {

		//// Start ////
		PDDocument pdfDocument = PDDocument.load(pdfFile);
		Document document = new Document(pdfDocument, pageNum, lineSpacingThreshold, spaceScale, roi);

		document.isolateMergedColumns();
		document.createNeighborhoods();
		document.mergeIsolateBlocks();
		document.decomposeType1Blocks();
		if(minRows != null) {
			document.removeNonTableNeighborhoods(minRows);
		}
		int neighborhoodNum = 0;

		int[] allNeighs = new int[document.getNeighborhoods().size()];
		for (int i = 0; i < document.getNeighborhoods().size(); i++) {
			allNeighs[i] = i;
		}
		if (debug) {
			drawNeighborhoods(pdfDocument, pageNum, document, debugDir, allNeighs);
			drawBlocks(pdfDocument, pageNum, document, debugDir);
			drawTiles(pdfDocument, pageNum, document, debugDir, neighborhoodNum);

			System.out.println("Making neighborhood #: " + (neighborhoodNum + 1) + "/"
					+ document.getNeighborhoods().size() + " into HTML table.");
		}

		PrintWriter writer = new PrintWriter(outputFilePath, "UTF-8");
		writer.print(document.getTableString(neighborhoodNum)); // need to determine the right neighborhood to print
		writer.close();
		System.out.println("HTML file created: " + outputFilePath);

		pdfDocument.close();

		// TableFinder -> rulings
		// -> TableExtractor
		// -> CSVWriter
	}

	/// DRAWING ///

	public static void drawROI(File pdfFile, int pageNum, String outputFilePath, Rectangle2D roi) throws IOException {
		PDDocument pdfDocument = PDDocument.load(pdfFile);
		PDPageContentStream contentStream = getContentStream(pdfDocument, pageNum);
		contentStream.setStrokingColor(Color.RED);
		contentStream.addRect((float) roi.getX(), (float) roi.getY(), (float) roi.getWidth(), (float) roi.getHeight());
		contentStream.stroke();
		contentStream.close();
		pdfDocument.save(outputFilePath);
		pdfDocument.close();
	}

	public static void drawNeighborhoods(PDDocument pdfDocument, int pageNum, Document document, String outputDir,
			int... which) throws IOException {
		PDPageContentStream contentStream = getContentStream(pdfDocument, pageNum);
		int neighNum = 0;
		for (int neighIndex : which) {
			Neighborhood n = document.getNeighborhoods().get(neighIndex);
			Tile[][] tiles = n.getTiles();
			if (tiles == null) {
				drawBlocks(contentStream, MyUtils.getKellyColor(neighNum++), n);
				continue;
			}
			List<Tile> tiles2 = new ArrayList<Tile>();
			for (int i = 0; i < tiles.length; i++) {
				for (int j = 0; j < tiles[i].length; j++) {
					tiles2.add(tiles[i][j]);
				}
			}
			List<Rectangle> ns = new ArrayList<Rectangle>();
			ns.add(n);
			ns.addAll(tiles2);
			Rectangle[] r = n.getType1Blocks().toArray(new Rectangle[n.getType1Blocks().size()]);
			drawBlocks(contentStream, MyUtils.getKellyColor(neighNum++), r);
		}
		contentStream.close();
		flipContentStreamBack(pdfDocument, pageNum);
		pdfDocument.save(outputDir + "neighborhoodDrawing.pdf");
	}

	public static void drawTiles(PDDocument pdfDocument, int pageNum, Document document, String outputDir, int... which)
			throws IOException {
		PDPageContentStream contentStream = getContentStream(pdfDocument, pageNum);
		int colorNum = 0;
		for (int neighIndex : which) {
			Neighborhood n = document.getNeighborhoods().get(neighIndex);
			Tile[][] tiles = n.getTiles();
			if (tiles == null) {
				// drawBlocks(contentStream, MyUtils.getKellyColor(colorNum++), n);
				continue;
			}
			List<Tile> tiles2 = new ArrayList<Tile>();
			for (int i = 0; i < tiles.length; i++) {
				for (int j = 0; j < tiles[i].length; j++) {
					tiles2.add(tiles[i][j]);
				}
			}
			List<Rectangle> ns = new ArrayList<Rectangle>();
			ns.add(n);
			ns.addAll(tiles2);
			Rectangle[] r = ns.toArray(new Rectangle[ns.size()]);
			drawBlocks(contentStream, MyUtils.getKellyColor(colorNum++), r);
		}
		contentStream.close();
		flipContentStreamBack(pdfDocument, pageNum);
		pdfDocument.save(outputDir + "tileDrawing.pdf");
	}

	public static void drawBlocks(PDDocument pdfDocument, int pageNum, Document document, String outputDir)
			throws IOException {
		PDPageContentStream contentStream = getContentStream(pdfDocument, pageNum);
		drawBlocks(contentStream, Color.RED, document.getBlocks().toArray(new Block[document.getBlocks().size()]));
		contentStream.close();
		flipContentStreamBack(pdfDocument, pageNum);
		pdfDocument.save(outputDir + "blockDrawing.pdf");
	}

	public static PDPageContentStream getContentStream(PDDocument pdfDocument, int pageNum)
			throws IOException {
		// drawBlocks(pdfDocument, pageNum, document.getBlocks(), Color.RED);
		PDPage page = pdfDocument.getPage(pageNum);
		Matrix rotationMatrix = getRotationMatrix(page.getRotation(), page.getCropBox().getWidth(),
				page.getCropBox().getHeight());
		if (rotationMatrix == null) {
			System.err.println("Unsupported rotation angle (i.e. not 0, 90, 180, or 270.");
			return null;
		}
		PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page,
				PDPageContentStream.AppendMode.APPEND, true);
		contentStream.transform(rotationMatrix);
		contentStream.setLineWidth(1.0f);
		return contentStream;
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

	public static void drawBlocks(PDPageContentStream contentStream, Color c, Rectangle... rects) throws IOException {
		contentStream.setStrokingColor(c);
		for (Rectangle b : rects) {
			contentStream.addRect(b.x, b.y, b.width, b.height);
		}
		contentStream.stroke();
	}

	public static void displayImage(BufferedImage img) {
		JFrame frame = new JFrame();
		ImageIcon icon = new ImageIcon(img);
		JLabel label = new JLabel(icon);
		frame.add(label);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * If a page is rotated, the content stream is rotated
	 * transform(AffineTransform) so that when we draw on it, the drawing
	 * coordinates take into account the rotation of the page. Drawing multiple
	 * times, however, results in the content stream rotating each time. In effect,
	 * every other call to a draw() function will be rotated wrongly. It works fine
	 * if you just call a draw() function once, but the next call will rotate it
	 * back.
	 * 
	 * This function simply performs the rotation without drawing anything.
	 * 
	 * @throws IOException
	 */
	public static void flipContentStreamBack(PDDocument pdfDocument, int pageNum)
			throws IOException {
		PDPageContentStream contentStream = getContentStream(pdfDocument, pageNum);
		contentStream.close();
	}
}
