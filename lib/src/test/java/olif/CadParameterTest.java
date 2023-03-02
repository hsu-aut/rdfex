package olif;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import olif.xml.XmlMappingResult;

public class CadParameterTest {

	static MappingEngine mappingEngine;
	ModelCache modelCache = ModelCache.getInstance();

	/**
	 * Should take the file "cadParametersBeforeMapping.xml" and insert information of the ontology (nameplate-mapping.ttl) as parameters. The result
	 * should then look like "cadParametersAfterMapping.xml".
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws TransformerFactoryConfigurationError 
	 * @throws TransformerException 
	 */
	@Test
	void shouldMapCadParameters() throws ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
		Path outputPath = Paths.get("src", "test", "resources", "xml", "cad-parameters", "cadParametersBeforeMapping.xml").toAbsolutePath();
		Path mappingPath = Paths.get("src", "test", "resources", "xml", "cad-parameters", "cadParameters-mapping.ttl").toAbsolutePath();
		Path expectedResultPath = Paths.get("xml/cad-parameters/cadParametersAfterMapping.xml");

		mappingEngine = new MappingEngine(mappingPath);
		// Create the mapped document according to the mapping definition
		List<MappingResult> mappingResults = mappingEngine.map(outputPath);
		MappingResult onlyResult = mappingResults.get(0);
		Document mappedDoc = ((XmlMappingResult) onlyResult).getDocument();

		// Write mapped doc to file
		File mappedFile = File.createTempFile("mappedDoc", null);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		DOMSource source = new DOMSource(mappedDoc);
		FileWriter writer = new FileWriter(mappedFile);
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
		
		// Load the expected document
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream(expectedResultPath.toString());
		File expectedFile = File.createTempFile("temp", null);
		OutputStream outputStream = new FileOutputStream(expectedFile);
		IOUtils.copy(is, outputStream);
		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document expectedDoc = documentBuilder.parse(expectedFile);
		
		// Compare actual mapped with expected document
		XmlAssert.assertThat(mappedDoc).and(expectedDoc)
		.ignoreWhitespace()
    	.normalizeWhitespace()
    	.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.Default))
    	.areSimilar();
	}
}
