package olif;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A simple factory class that provides a suitable {@link SourceModel} for a given source type.
 */
public class SourceModelFactory {

	
	public static SourceModel getModel(String sourceType, String source, Path mappingFileDirectory) {
		switch (sourceType) {
		case "http://www.hsu-hh.de/aut/ontologies/olif#File":
			Path mappingSourcePath = mappingFileDirectory.resolve(Paths.get(source));
			return new FileModel(mappingSourcePath);
		case "http://www.hsu-hh.de/aut/ontologies/olif#SparqlEndpoint":
			return new EndpointModel(source);
		default:
			throw new Error("The invalid sourceType "+ sourceType + " has been given. Please select a valid sourceType, i.e. one of the individuals of the class SourceType.");
		}
	}
}
