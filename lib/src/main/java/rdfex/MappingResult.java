package rdfex;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class MappingResult {

	protected Path outputPath;
	
	public abstract void writeResult();
	public abstract String getStringResult();
	
	
	/**
	 * Converts a filePath with a file extension to a path without extension
	 * @param filePath Path to a file that might contain a file extension (e.g. ".json")
	 * @return Path without extension
	 */
	protected Path removeFileExtension(Path filePath) {
	    String fileName = filePath.toString();
		if (fileName == null || fileName.isEmpty()) {
	        return Paths.get(fileName);
	    }

	    String extPattern = "(?<!^)[.][^.]*$";
	    String changedFileName = fileName.replaceAll(extPattern, "");
	    return Paths.get(changedFileName);
	}
}
