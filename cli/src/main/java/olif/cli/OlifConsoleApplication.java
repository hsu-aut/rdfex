package olif.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import olif.MappingEngine;
import olif.MappingResult;

public class OlifConsoleApplication {
	MappingEngine mappingEngine = new MappingEngine();

	public void run(String[] args) {
		CommandLine line = parseArguments(args);
		
		if(line.hasOption("help")) {
			printHelp();
			return;
		}
		
		if (line.hasOption("mappingPath") && line.hasOption("outputPath")) {
			System.out.println("Started Mapping...");
			Path mappingPath = Paths.get(line.getOptionValue("mappingPath")).normalize().toAbsolutePath();
			Path outputPath = Paths.get(line.getOptionValue("outputPath")).normalize().toAbsolutePath();
			System.out.println("MappingFile: " + mappingPath + "\nOutputPath: " + outputPath + "\n");
			List<MappingResult> mappingResults = mappingEngine.map(mappingPath, outputPath);
			for (MappingResult mappingResult : mappingResults) {
				mappingResult.writeResult();
			}
		} else {
			System.out.println("Missing parameters -m and -o...");
			printHelp();
		}
	}

	private CommandLine parseArguments(String[] args) {
		Options options = getOptions();
		CommandLine line = null;

		CommandLineParser parser = new DefaultParser();

		try {
			line = parser.parse(options, args);

		} catch (ParseException ex) {

			System.err.println("Failed to parse command line arguments");
			System.err.println(ex.toString());

			System.exit(1);
		}
		return line;
	}

	private Options getOptions() {
		Options options = new Options();
		options.addOption("h", "help", false, "Print help");
		options.addOption("m", "mappingPath", true, "Path to a file containing mapping definitions.");
		options.addOption("o", "outputPath", true, "Path to the output file");

		return options;
	}

	/**
	 * Prints application help with all possible arguments
	 */
	private void printHelp() {

		Options options = getOptions();

		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar olif-cli.jar", options, true);
	}

}
