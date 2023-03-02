package olif.json;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import olif.MappingEngine;
import olif.MappingResult;
import olif.ModelCache;

public class NameplateTest {

	static MappingEngine mappingEngine;
	ModelCache modelCache = ModelCache.getInstance();

	/**
	 * Should take the file "nameplateBeforeMapping.json" and insert information of the ontology (nameplate-mapping.ttl) into the two value fields of the
	 * properties "DateOfManufacture" and "SerialNumber". The result should then look like "nameplateAfterMapping.json".
	 * 
	 * @throws IOException
	 * @throws JSONException
	 */
	@Test
	void shouldMapIntoNameplate() throws IOException, JSONException {
		Path outputPath = Paths.get("src", "test", "resources", "json", "nameplate", "nameplateBeforeMapping.json").toAbsolutePath();
		Path mappingPath = Paths.get("src", "test", "resources", "json", "nameplate", "nameplate-mapping.ttl").toAbsolutePath();
		Path expectedResultPath = Paths.get("src", "test", "resources", "json", "nameplate", "nameplateAfterMapping.json").toAbsolutePath();

		mappingEngine = new MappingEngine(mappingPath);
		// Create the mapped document according to the mapping definition
		List<MappingResult> mappingResults = mappingEngine.map(outputPath);
		JsonElement actualResult = ((JsonMappingResult) mappingResults.get(0)).json;

		// Load the expected document
		JsonElement expectedResult = JsonParser.parseReader(new FileReader(expectedResultPath.toFile()));
		// Compare actual mapped with expected document
		JSONAssert.assertEquals(expectedResult.toString(), actualResult.toString(), false);
	}

}
