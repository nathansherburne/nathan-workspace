package pdfop;

import java.io.File;
import java.io.IOException;
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
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;

import technology.tabula.TextElement;
import technology.tabula.TextStripper;
import technology.tabula.Utils;

public class PdfOp {
	PDDocument pdfDocument;
	List<Integer> pages;
	String outPath;
	
	public PdfOp(PDDocument pdfDocument, List<Integer> pages, String outPath) {
		this.pdfDocument = pdfDocument;
		this.pages = pages;
		this.outPath = outPath;
	}
	
	public static void main(String[] args) throws InvalidPasswordException, IOException {
		Options options = new Options();

		Option input = Option.builder("i").argName("input").hasArg().desc("The input PDF file(s)").build();
		options.addOption(input);

		Option output = new Option("o", "output", true, "output file (PDF)");
		output.setRequired(true);
		options.addOption(output);

		Option pageOpt = new Option("p", "page", true, "page number(s). Either a single number or comma separated list. (Default is all pages)");
		pageOpt.setRequired(false);
		options.addOption(pageOpt);
		
		Option textOnlyOp = new Option("k", "keep-text", false, "extract text only from the PDF and save in the output directory");
		textOnlyOp.setRequired(false);
		options.addOption(textOnlyOp);
		
		Option noTextOp = new Option("r", "remove-text", false, "extract text only from the PDF and save in the output directory");
		noTextOp.setRequired(false);
		options.addOption(noTextOp);
		
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
		
		String inputFile = cmd.getOptionValue("i");
		String outputPath = cmd.getOptionValue("output");
		List<Integer> pages = new ArrayList<>();

		if (cmd.hasOption("page")) {
			String value = cmd.getOptionValue("page");
			String[] pageNums = value.split("[,]");
			for(int i = 0; i < pageNums.length; i++) {
				pages.add(Integer.valueOf(pageNums[i].trim()) - 1); 
			}
			if(pages.size() == 0) {
				pages.add(-1);
			}
		} else {
			pages.add(-1);  // Default is all pages
		}
		PDDocument pdfDocument = PDDocument.load(new File(inputFile));
		if(pages.get(0) == -1) {  // Default is all pages.
			pages.remove(0);
			for(int i = 0; i < pdfDocument.getNumberOfPages(); i++) {
				pages.add(i);
			}
		}
		PdfOp pdfOp;
		PDDocument doc;
		
		if(cmd.hasOption("keep-text")) {
			pdfOp = new PdfOp(pdfDocument, pages, outputPath);
			doc = pdfOp.getJustTextDoc();
			doc.save(outputPath);
			doc.close();
		}
		if(cmd.hasOption("remove-text")) {
			pdfOp = new PdfOp(pdfDocument, pages, outputPath);
			doc = pdfOp.getNoTextDoc();
			doc.save(outputPath);
			doc.close();
		}
		pdfDocument.close();
	}
		
	private PDDocument getJustTextDoc() {	
		PDDocument document = new PDDocument();
		try {
		for(int pageNum : pages) {
			ArrayList<TextElement> tes = extractText(pdfDocument, pageNum);
			// Set up new page's meta info and rotation
			PDPage myPage = new PDPage();
			PDPage origPage = pdfDocument.getPage(pageNum);
			copyMetaInfo(origPage, myPage);
			PDPageContentStream contentStream = new PDPageContentStream(document, myPage,
					PDPageContentStream.AppendMode.APPEND, true);
			Matrix rotationMatrix = getRotationMatrix(myPage.getRotation(), myPage.getCropBox().getWidth(),
					myPage.getCropBox().getHeight());
			contentStream.transform(rotationMatrix);
			// Something is wrong with my Affine Transformations. That is, everything is flipped upside down.
			// So here I flip it vertically.
			contentStream.transform(getRotationMatrix(0, myPage.getCropBox().getWidth(),
					myPage.getCropBox().getHeight()));
			
			// Draw all the text onto the new page
			for(TextElement te : tes) {
				contentStream.beginText();
				contentStream.newLineAtOffset((float) te.getMinX(), myPage.getCropBox().getHeight() - (float) te.getMaxY());
				float fontSize = te.getFontSize();
				if(Float.compare(fontSize, 1.0f) == 0) {
					fontSize = (int) (te.getHeight() / (myPage.getCropBox().getHeight()) * 1350);  // Sometimes font size is 1.0. So approximate it.
				}
				contentStream.setFont(te.getFont(), fontSize);
				try {
					contentStream.showText(te.getText());
				} catch(IllegalArgumentException iae) {
					System.err.println("Can't draw character: \"" + te.getText() + "\" (not available in the selected font). Printing in Times New Roman...");
					contentStream.setFont(PDType1Font.TIMES_ROMAN, fontSize);
					contentStream.showText(te.getText());
				}
				contentStream.endText();
			}
			contentStream.close();
			document.addPage(myPage);
		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return document;
	}
	
	/**
	 * 
	 * @return a PDDocument with all the text removed.
	 * @throws IOException
	 */
	private PDDocument getNoTextDoc() throws IOException {
		PDDocument noTextDoc = new PDDocument();
		for(int pageNum : pages) {
			noTextDoc.importPage(pdfDocument.getPage(pageNum));
		}
		RemoveAllText textRemover = new RemoveAllText(noTextDoc);
		return textRemover.getNoTextDocument();
	}
	
	/**
	 * Copies relevant meta info from the input page to the custom page.
	 * @param from
	 * @param to
	 */
	public void copyMetaInfo(PDPage from, PDPage to) {
		to.setCropBox(from.getCropBox());
		to.setRotation(from.getRotation());
		to.setMediaBox(from.getMediaBox());
		to.setResources(from.getResources());
	}
	
	private ArrayList<TextElement> extractText(PDDocument pdfDocument, int pageNum) {
		TextStripper textStripper = null;
		try {
			textStripper = new TextStripper(pdfDocument, pageNum + 1);
			textStripper.process();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (textStripper.textElements.size() == 0) {
			System.err.println("Could not extract text from the PDF.");
			System.exit(1);
		}
		// Sort and then combine characters into words (that's why characters must be
		// sorted, since
		// they will be evaluated sequentially).
		Utils.sort(textStripper.textElements);
		return textStripper.textElements;
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

	public static Matrix getRotationMatrix(int rotation, float width, float height) throws IOException {
		if (rotation == 0) {
			// return new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, 1, 0, 0)); //
			// Identity matrix
			// Not sure why, but the identity matrix yields a vertically flipped result.
			// So flip the PDF if its rotation is 0.
			return new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, -1, 0, height)); // Vertical mirror
		}
		if (rotation == 90) {
			return new Matrix(new java.awt.geom.AffineTransform(0, 1, 1, 0, 0, 0)); // Rotate counter-clockwise

		}
		// 180
		// 270
		return null; //

	}
}
