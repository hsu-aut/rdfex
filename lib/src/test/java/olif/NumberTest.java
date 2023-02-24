package olif;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import olif.xml.XmlMappingResult;

public class NumberTest {

	static MappingEngine mappingEngine;
	static Path mappingPath = Paths.get("src", "test", "resources", "xml", "regex-testing", "mapping.ttl").toAbsolutePath();
	ModelCache modelCache = ModelCache.getInstance();

	@BeforeAll
	static void setUp() throws Exception {
		mappingEngine = new MappingEngine(mappingPath);
	}


	@Test
	void shouldMapNumbers() throws ParserConfigurationException, SAXException, IOException {
		// Create the mapped document according to the mapping definition
		Path outputPath = Files.newTemporaryFile().toPath();
		List<MappingResult> mappingResults = mappingEngine.map(outputPath);
		MappingResult onlyResult = mappingResults.get(0);
		Document mappedDoc = ((XmlMappingResult) onlyResult).getDocument();
		
		// Load the expected document
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("xml/regex-testing/numbers.xml");
		File expectedFile = File.createTempFile("temp", null);
		OutputStream outputStream = new FileOutputStream(expectedFile);
		IOUtils.copy(is, outputStream);
		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document expectedDoc = documentBuilder.parse(expectedFile);
				
		// Compare actual mapped with expected document
	    XmlAssert.assertThat(mappedDoc).and(expectedDoc)
	    	.ignoreWhitespace()
	    	.normalizeWhitespace()
	    	.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
	    	.areSimilar();
	}
	

}
	
