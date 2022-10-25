package olif;

import olif.json.JsonMapper;
import olif.xml.XmlMapper;

/**
 * A simple factory class that provides a suitable Mapper for a given target format.
 */
public class SourceModelFactory {

	
	public static SourceModel getModel(String sourceType, String source) {
		switch (sourceType) {
		case "http://www.hsu-hh.de/aut/ontologies/olif#File":
			return new FileModel(source);
		case "http://www.hsu-hh.de/aut/ontologies/olif#SparqlEndpoint":
			return new EndpointModel(source);
		default:
			throw new Error("The invalid sourceType "+ sourceType + " has been given. Please select a valid sourceType, i.e. one of the individuals of the class SourceType.");
		}
	}
}
