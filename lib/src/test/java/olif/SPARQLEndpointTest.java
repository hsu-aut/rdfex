package olif;

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
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.assertj.XmlAssert;

import olif.xml.XmlMappingResult;

class SPARQLEndpointTest {
	
	static Path mappingPath = Paths.get("src", "test", "resources", "persons", "mapping_Endpoint.ttl").toAbsolutePath();
	static Path modelPath = Paths.get("src", "test", "resources", "persons", "persons.ttl").toAbsolutePath();
	static FusekiServer server;

	@BeforeEach
	void setUp() throws Exception {
		Dataset ds = DatasetFactory.createTxnMem() ;
		Model model = RDFDataMgr.loadModel(modelPath.toString());
		ds.setDefaultModel(model);
		server = FusekiServer.create().add("/ds", ds).build() ;
		server.start() ;
		System.out.println("Server is running");
	}

	@Test
	void test() throws IOException, ParserConfigurationException, SAXException {
		MappingEngine mappingEngine = new MappingEngine(mappingPath);
		// Create the mapped document according to the mapping definition
		Path outputPath = Files.newTemporaryFile().toPath();
		List<MappingResult> mappingResults = mappingEngine.map(outputPath);
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
		XmlAssert.assertThat(mappedDoc).and(expectedDoc).ignoreWhitespace().areSimilar();
		
	}
	
	@AfterAll
	static void tearDown() {
		server.stop();
	}

}
