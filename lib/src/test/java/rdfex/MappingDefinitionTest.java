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

import rdfex.DataMap;
import rdfex.MappingEngine;
import rdfex.ModelCache;
import rdfex.xml.XmlMappingResult;

class MappingDefinitionTest {

	static MappingEngine mappingEngine;
	static Path mappingPath = Paths.get("src", "test", "resources", "xml", "mapping.ttl").toAbsolutePath();
	ModelCache modelCache = ModelCache.getInstance();

	@BeforeAll
	static void setUp() throws Exception {
		mappingEngine = new MappingEngine(mappingPath);
	}


	/**
	 * The mapping definition test contains two mapping definitions, these should be found
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

	
}