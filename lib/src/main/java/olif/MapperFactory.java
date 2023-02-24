package olif;

import olif.json.JsonMapper;
import olif.xml.XmlMapper;

/**
 * A simple factory class that provides a suitable Mapper for a given target format.
 */
public class MapperFactory {

	
	public static Mapper getMapper(String targetFormat) {
		switch (targetFormat) {
		case "http://www.hsu-hh.de/aut/ontologies/olif#JSON":
			return new JsonMapper();
		case "http://www.hsu-hh.de/aut/ontologies/olif#XML":
			return new XmlMapper();
		default:
			throw new Error("The invalid targetFormat "+ targetFormat + " has been given. Please select a valid target format, i.e. one of the individuals of the class DataExchangeFormat.");
		}
	}
}
