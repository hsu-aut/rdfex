package olif;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.assertj.XmlAssert;

import olif.xml.XmlMappingResult;

class PersonTest {

	static MappingEngine mappingEngine;
	static Path mappingPath = Paths.get("src", "test", "resources", "persons", "mapping.ttl");
	ModelCache modelCache = ModelCache.getInstance();

	@BeforeAll
	static void setUp() throws Exception {
		mappingEngine = new MappingEngine();
	}


	/**
	 * The person test contains two mapping definitions, these should be found
	 */
	@Test
	void shouldGiveTwoMappings() {
		// Tests whether model cache returns the correct model and whether two mappings are found
		// TODO: This test could be broken down so that both aspects are tested separately 
		// (one for modelcache, one for getting right number of mappings
		Model model = this.modelCache.getModel(mappingPath);
		List<DataMap> mappings = mappingEngine.getAllMappingDefinitions(model);
		
		assertEquals(2, mappings.size());
	}

	
	/**
	 * The persons mapping only has the XML target format, thus there should only be one mapping result
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	@Test
	void shouldGiveOneMappingResult() throws ParserConfigurationException, SAXException, IOException {
		// Create the mapped document according to the mapping definition
		Path outputPath = Files.newTemporaryFile().toPath();
		List<MappingResult> mappingResults = mappingEngine.map(mappingPath, outputPath);
		
		assertEquals(1, mappingResults.size());
	}
	
	
	/**
	 * Inspects the mapping result of the persons maping
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	@Test
	void shouldMapPersons() throws ParserConfigurationException, SAXException, IOException {
		// Create the mapped document according to the mapping definition
		Path outputPath = Files.newTemporaryFile().toPath();
		List<MappingResult> mappingResults = mappingEngine.map(mappingPath, outputPath);
		MappingResult onlyResult = mappingResults.get(0);
		Document mappedDoc = ((XmlMappingResult) onlyResult).getDocument();
		
		// Load the expected document
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("persons/persons.xml");
		File expectedFile = File.createTempFile("temp", null);
		OutputStream outputStream = new FileOutputStream(expectedFile);
		IOUtils.copy(is, outputStream);
		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document expectedDoc = documentBuilder.parse(expectedFile);

		// Compare actual mapped with expected document
	    XmlAssert.assertThat(expectedDoc).and(mappedDoc)
	    	.ignoreWhitespace()
	    	.areSimilar();
	}

}
