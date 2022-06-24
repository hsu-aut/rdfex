package olif;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MappingEngine {

	// Output XML document
	Document doc;

	public Document map(Path mappingFilePath) throws ParserConfigurationException {
		// Create mapping model
		Model mappingModel = this.getModelFromFile(mappingFilePath);

		// Create empty XML file
		DocumentBuilder docBuilder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();
		this.doc = docBuilder.newDocument();

		// Get all mapping definitions
		List<DataMap> mappings = this.getAllMappingDefinitions(mappingModel);

		// For each mapping:
		for (DataMap mappingDefinition : mappings) {
			// Get the appropriate mapper
			String targetFormat = mappingDefinition.getTargetFormat();
			System.out.println(targetFormat);
			Mapper mapper = MapperFactory.getMapper(targetFormat);

			// open source file
			// TODO (performance optimization) Put models in some kind of cache to not load one model twice
			String mappingSource = mappingDefinition.getSource();
			Path directory = mappingFilePath.getParent();
			Path mappingSourcePath = directory.resolve(Paths.get(mappingSource));
			// Path mappingSourcePath = Paths.get(directory, mappingSource);
			Model sourceModel = getModelFromFile(mappingSourcePath);

			// fire SPARQL query
			// TODO: Add all prefixes from mapping document into query header
			String queryString = mappingDefinition.getQuery();
			Query mappingQuery = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.create(mappingQuery, sourceModel);

			ResultSet resultSet = qexec.execSelect();
			List<QuerySolution> results = ResultSetFormatter.toList(resultSet);

			// execute XPath to get the container (with placeholders)
			String containerString = mappingDefinition.getContainer();
			// fill possible placeholders
			List<String> containers = this.fillPlaceholders(containerString, results);

			for (String container : containers) {
				XPath xPath = XPathFactory.newDefaultInstance().newXPath();

				NodeList containerNodes = null;
				try {
					containerNodes = (NodeList) xPath.compile(containerString).evaluate(doc, XPathConstants.NODESET);

					// Create new container nodes if XPath doesn't return any
					if (containerNodes.getLength() == 0) {
						Node containerStructure = this.createContainerStructure(container);
						doc.appendChild(containerStructure);
					}
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String snippet = mappingDefinition.getSnippet();
				List<String> completedSnippets = this.fillPlaceholders(snippet, results);

//				// for each container result
//				for (int i = 0; i < containerNodes.getLength(); i++) {
//					Node containerNode = containerNodes.item(i);
//					
//					// add enriched snippet to XML
//					for (String completedSnippet : completedSnippets) {
//						containerNode.setTextContent(completedSnippet);
//						doc.appendChild(containerNode);
//					}
//					
//				}

			}

		}

		return doc;
	}

	/**
	 * Create a Jena model from the given mapping Turtle file
	 * 
	 * @param path Path to the mapping definition
	 * @return Jena model of the mapping definition
	 */
	public Model getModelFromFile(Path path) {
		// Create a model from the mapping file
		InputStream in = RDFDataMgr.open(path.toString());
		if (in == null) {
			throw new IllegalArgumentException("File: " + path + " not found");
		}

		Model model = ModelFactory.createDefaultModel();
		// read the file
		// TODO: Get the real file format
		model.read(in, null, "TTL");
		return model;
	}

	
	/**
	 * Retrieves all mapping definitions inside the current mapping and converts them into an easily accessible object that represents mapping definitions
	 * 
	 * @return All mapping definitions
	 */
	public List<DataMap> getAllMappingDefinitions(Model model) {
		String queryString = "PREFIX ol: <http://www.hsu-hh.de/aut/ontologies/olif#>"
				+ "SELECT ?mappingDefinition ?source ?sourceType ?targetFormat ?queryLanguage ?query ?container ?snippet WHERE {"
				+ "?mappingDefinition ol:ontologicalSource ?ontologicalSource." + "?ontologicalSource ol:source ?source;" + "ol:sourceType ?sourceType;"
				+ "ol:queryLanguage ?queryLanguage;" + "ol:query ?query." + "?mappingDefinition ol:container ?container;" + "ol:targetFormat ?targetFormat;"
				+ "ol:snippet ?snippet." + "}";

		Query mappingQuery = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(mappingQuery, model);

		ResultSet results = qexec.execSelect();
		List<QuerySolution> querySolutions = ResultSetFormatter.toList(results);

		List<DataMap> dataMaps = querySolutions.stream().map(qS -> new DataMap(qS)).collect(Collectors.toList());
		return dataMaps;
	}

	
	/**
	 * Take a string containing ${?...} placeholders and fill it with sparql results
	 * 
	 * @param stringWithPlaceholder
	 * @param sparqlResults
	 * @return
	 */
	public List<String> fillPlaceholders(String stringWithPlaceholder, List<QuerySolution> sparqlResults) {
		// Get all template strings
		List<String> allMatches = this.findAllRegexMatches(stringWithPlaceholder, "(\\$\\{\\?\\w*\\})");

		List<String> completedStrings = new ArrayList<String>();
		for (QuerySolution result : sparqlResults) {
			String completedString = stringWithPlaceholder;
			for (String match : allMatches) {
				String varName = match.substring(3, match.length() - 1);
				completedString = completedString.replace(match, result.get(varName).toString());
			}
			completedStrings.add(completedString);
		}
		return completedStrings;
	}

	// TODO: Continue with this method so that in the case of empty documents / non-found containers new containers are created
	private Node createContainerStructure(String container) {
		// Xpath \/(\w-*)+(\[.+\])?
//		Pattern pattern = Pattern.compile("(\\$\\{\\?\\w*\\})");
		Pattern pattern = Pattern.compile("\\/(\\w-*)+(\\[.+\\])?");
		Matcher matcher = pattern.matcher(container);
		// for every match: create a node and create a tree
		Node rootNode = null;
		Node parentNode = null;
		int counter = 0;
		while (matcher.find()) {
			// Clear matches from invalid characters
			String elementName = matcher.group().substring(1);
			// find complete brackets (\[.+\])?
			// separate attributes.
			// Assumption: Three groups are found
			// 1: Complete bracket: [@id = asd]
			// 2: Attribute only: id
			// 3: Value only: asd
			List<String> attributeMatches = this.findAllRegexMatches(elementName, "(\\[@(\\w+)=(.*)\\])");
			if (attributeMatches.size() > 0) {
				elementName = elementName.substring(0, elementName.length() - attributeMatches.get(1).length());
			}

			if (counter == 0) {
				rootNode = this.doc.createElement(elementName);
				if (attributeMatches.size() > 0) {
					String attributeName = attributeMatches.get(2);
					String attributeValue = attributeMatches.get(3);
					((Element) parentNode).setAttribute(attributeName, attributeValue);
				}
				parentNode = rootNode;
			}

			if (counter > 0) {
				Node node = this.doc.createElement(elementName);
				if (attributeMatches.size() > 0) {
					String attributeName = attributeMatches.get(2);
					String attributeValue = attributeMatches.get(3);
					((Element) node).setAttribute(attributeName, attributeValue);
				}
				parentNode.appendChild(node);
				parentNode = node;
			}
			counter++;
		}
		return rootNode;
	}

	private List<String> findAllRegexMatches(String stringToSearch, String patternString) {
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(stringToSearch);
		List<String> allMatches = new ArrayList<String>();
		while (matcher.find()) {
			for (int i = 0; i <= matcher.groupCount(); i++) {
				allMatches.add(matcher.group(i));
			}
		}
		return allMatches;
	}

}
