package driver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.opencv.core.Core;

import detection.CVDetectionAlgorithm;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Rectangle;
import technology.tabula.Table;
import technology.tabula.Utils;
import technology.tabula.detectors.DetectionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import technology.tabula.writers.CSVWriter;
import technology.tabula.writers.Writer;

public class MyCommandLineApp {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	private static String VERSION = "1.0.2 (With OpenCV Detection)";
	private static String VERSION_STRING = String.format("tabula %s (c) 2012-2017 Manuel Aristarán", VERSION);
	private static String BANNER = "\nTabula helps you extract tables from PDFs\n\n";

	private Appendable defaultOutput;
	private List<Integer> pages;
	private String password;
	private TableExtractor tableExtractor;
	private String outputDirectory;

	public MyCommandLineApp(Appendable defaultOutput, CommandLine line) throws ParseException {
		this.defaultOutput = defaultOutput;
		this.pages = MyCommandLineApp.whichPages(line);
		this.tableExtractor = MyCommandLineApp.createExtractor(line);
		this.outputDirectory = MyCommandLineApp.whichOutputDirectory(line);
		if (line.hasOption('s')) {
			this.password = line.getOptionValue('s');
		}
	}

	public static void main(String[] args) {
		CommandLineParser parser = new DefaultParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(buildOptions(), args);

			if (line.hasOption('h')) {
				printHelp();
				System.exit(0);
			}

			if (line.hasOption('v')) {
				System.out.println(VERSION_STRING);
				System.exit(0);
			}

			new MyCommandLineApp(System.out, line).extractTables(line);
		} catch (ParseException exp) {
			System.err.println("Error: " + exp.getMessage());
			System.exit(1);
		}
		System.exit(0);
	}

	public void extractTables(CommandLine line) throws ParseException {
		if (line.hasOption('b')) {
			if (line.getArgs().length != 0) {
				throw new ParseException("Filename specified with batch\nTry --help for help");
			}

			File pdfDirectory = new File(line.getOptionValue('b'));
			if (!pdfDirectory.isDirectory()) {
				throw new ParseException("Directory does not exist or is not a directory");
			}
			extractDirectoryTables(pdfDirectory);
			return;
		}

		if (line.getArgs().length != 1) {
			throw new ParseException("Need exactly one filename\nTry --help for help");
		}

		File pdfFile = new File(line.getArgs()[0]);
		if (!pdfFile.exists()) {
			throw new ParseException("File does not exist");
		}
		extractFile(pdfFile);
	}

	public void extractDirectoryTables(File pdfDirectory) throws ParseException {
		File[] pdfs = pdfDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".pdf");
			}
		});

		for (File pdfFile : pdfs) {
			extractFile(pdfFile);
		}
	}

	private void extractFile(File pdfFile) throws ParseException {
		PDDocument pdfDocument = null;
		try {
			pdfDocument = this.password == null ? PDDocument.load(pdfFile) : PDDocument.load(pdfFile, this.password);
			// TODO: PDFont font = PDType0Font.load(pdfDocument, new File("src/tests/resources/fonts/arial.ttf"));
			PageIterator pageIterator = getPageIterator(pdfDocument);
			List<Table> tables = new ArrayList<Table>();

			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				tables.addAll(tableExtractor.extractTables(page));
			}
			writeTables(tables, getOutputFilename(pdfFile));
		} catch (IOException e) {
			throw new ParseException(e.getMessage());
		} finally {
			try {
				if (pdfDocument != null) {
					pdfDocument.close();
				}
			} catch (IOException e) {
				System.out.println("Error in closing pdf document" + e);
			}
		}
	}

	private PageIterator getPageIterator(PDDocument pdfDocument) throws IOException {
		ObjectExtractor extractor = new ObjectExtractor(pdfDocument);
		return (pages == null) ? extractor.extract() : extractor.extract(pages);
	}

	// CommandLine parsing methods

	private static List<Integer> whichPages(CommandLine line) throws ParseException {
		String pagesOption = line.hasOption('p') ? line.getOptionValue('p') : "1";
		return Utils.parsePagesOption(pagesOption);
	}

	private static ExtractionMethod whichExtractionMethod(CommandLine line) {
		// -r/--spreadsheet [deprecated; use -l] or -l/--lattice
		if (line.hasOption('l')) {
			return ExtractionMethod.SPREADSHEET;
		}


		// -g/--guess or -t/--stream
		if (line.hasOption('g') || line.hasOption('t')) {
			return ExtractionMethod.BASIC;
		}
		return ExtractionMethod.DECIDE;
	}

	private static TableExtractor createExtractor(CommandLine line) throws ParseException {
		TableExtractor extractor = new TableExtractor();
		extractor.setGuess(line.hasOption('g'));
		extractor.setMethod(MyCommandLineApp.whichExtractionMethod(line));
		extractor.setUseLineReturns(line.hasOption('u'));

		return extractor;
	}

	// utilities, etc.

	public static List<Float> parseFloatList(String option) throws ParseException {
		String[] f = option.split(",");
		List<Float> rv = new ArrayList<Float>();
		try {
			for (int i = 0; i < f.length; i++) {
				rv.add(Float.parseFloat(f[i]));
			}
			return rv;
		} catch (NumberFormatException e) {
			throw new ParseException("Wrong number syntax");
		}
	}

	private static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("tabula", BANNER, buildOptions(), "", true);
	}

	@SuppressWarnings("static-access")
	public static Options buildOptions() {
		Options o = new Options();

		o.addOption("v", "version", false, "Print version and exit.");
		o.addOption("h", "help", false, "Print this help text.");
		o.addOption("g", "guess", false, "Guess the portion of the page to analyze per page.");
		o.addOption("l", "lattice", false, "Force PDF to be extracted using lattice-mode extraction (if there are ruling lines separating each cell, as in a PDF of an Excel spreadsheet)");
		o.addOption("t", "stream", false, "Force PDF to be extracted using stream-mode extraction (if there are no ruling lines separating each cell)");
		o.addOption("i", "silent", false, "Suppress all stderr output.");
		o.addOption("u", "use-line-returns", false, "Use embedded line returns in cells. (Only in spreadsheet mode.)");
		o.addOption("d", "debug", false, "Print detected table areas instead of processing.");
		o.addOption(Option.builder("b")
				.longOpt("batch")
				.desc("Convert all .pdfs in the provided directory.")
				.hasArg()
				.argName("DIRECTORY")
				.build());
		o.addOption(Option.builder("o")
				.longOpt("outfile")
				.desc("Output directory. Default is the the current working directory.")
				.hasArg()
				.argName("OUTFILE")
				.build());
		o.addOption(Option.builder("f")
				.longOpt("stdout")
				.desc("Print output to STDOUT")
				.hasArg()
				.argName("STDOUT")
				.build());
		o.addOption(Option.builder("s")
				.longOpt("password")
				.desc("Password to decrypt document. Default is empty")
				.hasArg()
				.argName("PASSWORD")
				.build());
		o.addOption(Option.builder("p")
				.longOpt("pages")
				.desc("Comma separated list of ranges, or all. Examples: --pages 1-3,5-7, --pages 3 or --pages all. Default is --pages 1")
				.hasArg()
				.argName("PAGES")
				.build());

		return o;
	}

	protected static class TableExtractor {
		private boolean guess = false;
		private boolean useLineReturns = false;
		private BasicExtractionAlgorithm basicExtractor = new BasicExtractionAlgorithm();
		private SpreadsheetExtractionAlgorithm spreadsheetExtractor = new SpreadsheetExtractionAlgorithm();
		private ExtractionMethod method = ExtractionMethod.BASIC;

		public TableExtractor() {
		}

		public void setGuess(boolean guess) {
			this.guess = guess;
		}

		public void setUseLineReturns(boolean useLineReturns) {
			this.useLineReturns = useLineReturns;
		}

		public void setMethod(ExtractionMethod method) {
			this.method = method;
		}

		public List<Table> extractTables(Page page) {
			ExtractionMethod effectiveMethod = this.method;
			if (effectiveMethod == ExtractionMethod.DECIDE) {
				effectiveMethod = spreadsheetExtractor.isTabular(page) ? ExtractionMethod.SPREADSHEET
						: ExtractionMethod.BASIC;
			}
			switch (effectiveMethod) {
			case BASIC:
				return extractTablesBasic(page);
			case SPREADSHEET:
				return extractTablesSpreadsheet(page);
			default:
				return new ArrayList<Table>();
			}
		}

		public List<Table> extractTablesBasic(Page page) {
			if (guess) {
				// guess the page areas to extract using a detection algorithm
				// currently we only have a detector that uses spreadsheets to find table areas
				DetectionAlgorithm detector = new CVDetectionAlgorithm();
				List<Rectangle> guesses = detector.detect(page);
				List<Table> tables = new ArrayList<Table>();

				for (Rectangle guessRect : guesses) {
					Page guess = page.getArea(guessRect);
					tables.addAll(basicExtractor.extract(guess));
				}
				return tables;
			}

			return basicExtractor.extract(page);
		}

		public List<Table> extractTablesSpreadsheet(Page page) {
			// TODO add useLineReturns
			return (List<Table>) spreadsheetExtractor.extract(page);
		}
	}

	private void writeTables(List<Table> tables, String outputfilename) throws IOException, ParseException {
		Writer writer = new CSVWriter();
		int i = 0;
		String[] tokens = outputfilename.split("\\.(?=[^\\.]+$)");
		for(Table table : tables) {
			String tablefilename = tokens[0] + "-table-" + Integer.toString(i++) + "." + tokens[1];
			Path absolute_path = Paths.get(outputDirectory, tablefilename);
			System.out.println(tablefilename);
			File table_file = new File(absolute_path.toString());
			BufferedWriter bufferedWriter = null;
	        try {
	            FileWriter fileWriter = new FileWriter(table_file);
	            bufferedWriter = new BufferedWriter(fileWriter);
	            table_file.createNewFile();
	            writer.write(bufferedWriter, table);
	        } catch (IOException e) {
	            throw new ParseException("Cannot create file " + table_file);
	        } finally {
	            if (bufferedWriter != null) {
	                try {
	                    bufferedWriter.close();
	                } catch (IOException e) {
	                    System.out.println("Error in closing the BufferedWriter" + e);
	                }
	            }
	        }
		}
	}

	private String getOutputFilename(File pdfFile) {
		String extension = ".csv";
		return pdfFile.getName().replaceFirst("(\\.pdf|)$", extension);
	}
	
	private static String whichOutputDirectory(CommandLine line) {
		if (line.hasOption('o')) {
			return line.getOptionValue('o');
		}
		return Paths.get(System.getProperty("user.dir")).toString();
	}

	private enum ExtractionMethod {
		BASIC, SPREADSHEET, DECIDE
	}

	private class DebugOutput {
		private boolean debugEnabled;

		public DebugOutput(boolean debug) {
			this.debugEnabled = debug;
		}

		public void debug(String msg) {
			if (this.debugEnabled) {
				System.err.println(msg);
			}
		}
	}
}