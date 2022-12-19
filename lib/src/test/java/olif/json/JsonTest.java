package olif.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xml.sax.SAXException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import olif.MappingEngine;
import olif.MappingResult;
import olif.ModelCache;

public class JsonTest {
	static MappingEngine mappingEngine;

	ModelCache modelCache = ModelCache.getInstance();


	/**
	 * The persons mapping only has the XML target format, thus there should only be one mapping result
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	@Test
	void shouldGiveOneMappingResult() throws ParserConfigurationException, SAXException, IOException {
		Path outputPath = Paths.get("src", "test", "resources", "pokemon", "pokemon_empty.json").toAbsolutePath();
		Path mappingPath_step1 = Paths.get("src", "test", "resources", "pokemon", "mapping_step1.ttl").toAbsolutePath();
		mappingEngine = new MappingEngine(mappingPath_step1);
		// Create the mapped document according to the mapping definition
		List<MappingResult> mappingResults = mappingEngine.map(outputPath);
		assertEquals(1, mappingResults.size());
	}
	
	
	/**
	 * Should create the basic pokemon structure without the stats
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws JSONException 
	 */
	@Test
	void shouldMapFirstStep() throws ParserConfigurationException, SAXException, IOException, JSONException {
		Path outputPath = Paths.get("src", "test", "resources", "pokemon", "pokemon_empty.json").toAbsolutePath();
		Path mappingPath_step1 = Paths.get("src", "test", "resources", "pokemon", "mapping_step1.ttl").toAbsolutePath();
		Path expectedResultPath_Step1 = Paths.get("src", "test", "resources", "pokemon", "pokemon_step1.json").toAbsolutePath();
		
		mappingEngine = new MappingEngine(mappingPath_step1);
		// Create the mapped document according to the mapping definition
		List<MappingResult> mappingResults = mappingEngine.map(outputPath);
		JsonElement actualResult = ((JsonMappingResult) mappingResults.get(0)).json;
		
		// Load the expected document
		JsonElement expectedResult = JsonParser.parseReader(new FileReader(expectedResultPath_Step1.toFile()));
		// Compare actual mapped with expected document
		JSONAssert.assertEquals(actualResult.toString(), expectedResult.toString(), false);
	}
	
	
	/**
	 * Should extend the basic structure from above with stats
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws JSONException 
	 */
	@Test
	void shouldMapSecondStep() throws ParserConfigurationException, SAXException, IOException, JSONException {
		Path outputPath = Paths.get("src", "test", "resources", "pokemon", "pokemon_step1.json").toAbsolutePath();
		Path mappingPath_step2 = Paths.get("src", "test", "resources", "pokemon", "mapping_step2.ttl").toAbsolutePath();
		Path expectedResultPath_Step2 = Paths.get("src", "test", "resources", "pokemon", "pokemon_step2.json").toAbsolutePath();
		
		mappingEngine = new MappingEngine(mappingPath_step2);
		// Create the mapped document according to the mapping definition
		List<MappingResult> mappingResults = mappingEngine.map(outputPath);
		JsonElement actualResult = ((JsonMappingResult) mappingResults.get(0)).json;
		
		// Load the expected document
		JsonElement expectedResult = JsonParser.parseReader(new FileReader(expectedResultPath_Step2.toFile(), Charset.forName("utf8")));
		
		System.out.println(expectedResult.toString());
		System.out.println(actualResult.toString());
		// Compare actual mapped with expected document
		JSONAssert.assertEquals(actualResult.toString(), expectedResult.toString(), false);
	}


}
