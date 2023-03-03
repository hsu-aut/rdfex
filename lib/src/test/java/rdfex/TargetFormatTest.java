package rdfex;

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

import rdfex.MappingEngine;
import rdfex.MappingResult;
import rdfex.ModelCache;
import rdfex.xml.XmlMappingResult;

class TargetFormatTest {

	static MappingEngine mappingEngine;
	static Path mappingPath = Paths.get("src", "test", "resources", "xml", "mapping.ttl").toAbsolutePath();
	ModelCache modelCache = ModelCache.getInstance();

	@BeforeAll
	static void setUp() throws Exception {
		mappingEngine = new MappingEngine(mappingPath);
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
		List<MappingResult> mappingResults = mappingEngine.map(outputPath);
		
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
		List<MappingResult> mappingResults = mappingEngine.map(outputPath);
		MappingResult onlyResult = mappingResults.get(0);
		Document mappedDoc = ((XmlMappingResult) onlyResult).getDocument();
		
		// Load the expected document
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("xml/persons.xml");
		File expectedFile = File.createTempFile("temp", null);
		OutputStream outputStream = new FileOutputStream(expectedFile);
		IOUtils.copy(is, outputStream);
		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document expectedDoc = documentBuilder.parse(expectedFile);

		// Compare actual mapped with expected document
	    XmlAssert.assertThat(mappedDoc).and(expectedDoc)
	    	.ignoreWhitespace()
//	    	.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.))
	    	.areSimilar();
	}

}
