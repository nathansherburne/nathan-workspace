package driver;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import processing.CustomGraphicsStreamEngine;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

/**
 * Detects tables in each page of a PDF and converts them to CSV.
 * 
 * Tables are detected via rectangles (visible and invisible). So if the PDF
 * does not use rectangles to delineate its tables, this program will not work.
 * 
 * The only data this has been successfully tested on is the Rio weekly data.
 * 
 * Much, much more work is required for this to be anywhere close to a general
 * solution, but it is a start.
 * 
 * TODO: distinguish between multiple tables on the same page. TODO: handle
 * "merged" cells (cells that span multiple rows/columns (usually in the
 * header)). TODO: handle PDF tables with no rectangles. TODO: check to see if
 * this program detects lines or just rectangles, or if there is a difference.
 * TODO: detect table boundaries in order to exclude "mixed" content (i.e. pages
 * with tables, paragraphs, images, etc...). TODO: improve efficiency. Most of
 * the point/line/rectangle analysis functions are O(n^2) or worse. Still pretty
 * fast on small tables, though. TODO: create a drawing of the
 * points/lines/rectangles superimposed on the actual PDF instead of on a blank
 * PNG. TODO: much, much more...
 * 
 * @author Nathan Sherburne
 *
 */
public class PDF2CSV {

	public static void main(String[] args) throws IOException {
		Options options = new Options();

		Option input = new Option("i", "input", true, "input file path");
		input.setRequired(true);
		input.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(input);

		Option output = new Option("o", "output", true, "output directory");
		output.setRequired(true);
		options.addOption(output);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("PDF2CSV", options);
			System.exit(1);
			return;
		}

		String output_directory = cmd.getOptionValue("output");

		for (String input_file_path : cmd.getOptionValues("input")) {
			File file = new File(input_file_path);
			String output_file_path = Paths.get(output_directory, FilenameUtils.removeExtension(file.getName()) + ".csv").toString(); 
			int i = 0;
			try (PDDocument doc = PDDocument.load(file)) {
				StringBuilder sb = new StringBuilder();
				for (PDPage page : doc.getPages()) {
					CustomGraphicsStreamEngine engine = new CustomGraphicsStreamEngine(page);

					engine.run();
					engine.printTable();
					sb.append(engine.getCSVString());

					// Draw a grid of page's MyPoints
					//BufferedImage grid_of_points = engine.drawGrid();
					//ImageIO.write(grid_of_points, "png",
							//new File(Paths.get(output_directory, "grid" + i++ + ".png").toString()));
				}
				PrintWriter pw = new PrintWriter(new File(output_file_path));
				pw.write(sb.toString());
				pw.close();
			}
		}
	}
}
