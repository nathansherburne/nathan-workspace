package driver;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.opencv.core.Core;

import technology.tabula.Rectangle;
import technology.tabula.TextStripper;
import technology.tabula.Utils;
import textProcessing.Document;
import textProcessing.Neighborhood;
import textProcessing.Tile;
import utils.MyUtils;

public class Driver {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(String[] args) throws InvalidPasswordException, IOException {
		//// Command Line Options ////
		Options options = new Options();

		Option input = new Option("i", "input", true, "input file path (PDF)");
		input.setRequired(true);
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

		String inputFilePath = cmd.getOptionValue("input");
		String outputFilePath = cmd.getOptionValue("output");
		int pageNum = Integer.valueOf(cmd.getOptionValue("page")) - 1;

		//// Start ////
		File pdf = new File(inputFilePath);
		PDDocument pdfDocument = PDDocument.load(pdf);

		// Create Characters
		TextStripper textStripper = new TextStripper(pdfDocument, pageNum + 1);
		textStripper.process();
		if (textStripper.textElements.size() == 0) {
			System.err.println("Could not extract text from the PDF.");
			System.exit(1);
		}

		// Sort and then combine characters into words (that's why characters must be
		// sorted, since
		// they will be evaluated sequentially).
		Utils.sort(textStripper.textElements);
		Document document = new Document();
		document.createWords(textStripper.textElements);
		document.createLines();
		document.createDummyLines();
		document.createBlocks();
		document.createNeighborhoods();
		document.mergeIsolateBlocks();
		document.decomposeType1Blocks();

		drawing(pdfDocument, pageNum, document);
		PrintWriter writer = new PrintWriter(outputFilePath, "UTF-8");
		writer.print(document.getTableString());
		writer.close();
		
		PDFRenderer renderer = new PDFRenderer(pdfDocument);
		BufferedImage im = renderer.renderImage(pageNum);
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
		Matrix rotationMatrix = getRotationMatrix(page.getRotation());
		if (rotationMatrix == null) {
			System.err.println("Unsupported rotation angle (i.e. not 0, 90, 180, or 270.");
			return;
		}
		PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page, PDPageContentStream.AppendMode.APPEND,
				true);
		contentStream.transform(rotationMatrix);
		
		contentStream.setLineWidth(1.0f);
		for (Neighborhood n : document.getNeighborhoods()) {
			Tile[][] tiles = n.getTiles();
			List<Tile> tiles2 = new ArrayList<Tile>();
			for (int i = 0; i < tiles.length; i++) {
				for (int j = 0; j < tiles[i].length; j++) {
					tiles2.add(tiles[i][j]);
				}
			}
			List<Rectangle> ns = new ArrayList<Rectangle>();
			ns.add(n);
			ns.addAll(tiles2);
			drawBlocks(contentStream, ns, MyUtils.KELLY_COLORS[c++]);
		}
		contentStream.close();
		//drawBlocks(pdfDocument, 0, document.getBlocks(), "src/tests/resources/draw2.pdf", Color.RED);
		pdfDocument.save("src/tests/resources/blockDrawing.pdf");

	}

	public static Matrix getRotationMatrix(int rotation) throws IOException {
		if (rotation == 0) {
			return new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, 1, 0, 0)); // Identity matrix
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
}
