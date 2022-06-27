package olif;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MappingEngine {

	// Output XML document
	Document doc;

	// Mmodel cache to store models once they're loaded to get them faster for subsequent uses of the same model
	ModelCache modelCache = ModelCache.getInstance();

	public Document map(Path mappingFilePath) throws ParserConfigurationException, SAXException, IOException {
		// Create mapping model
		Model mappingModel = this.modelCache.getModel(mappingFilePath);

		// Create empty XML file
		DocumentBuilder docBuilder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();
		this.doc = docBuilder.newDocument();

		// Get all mapping definitions
		List<DataMap> mappings = this.getAllMappingDefinitions(mappingModel);

		// For each mapping:
		for (DataMap mappingDefinition : mappings) {
			// Get the appropriate mapper
			String targetFormat = mappingDefinition.getTargetFormat();
			Mapper mapper = MapperFactory.getMapper(targetFormat);

			// open source file
			// TODO (performance optimization) Put models in some kind of cache to not load one model twice
			String mappingSource = mappingDefinition.getSource();
			Path mappingFileDirectory = mappingFilePath.getParent();
			Path mappingSourcePath = mappingFileDirectory.resolve(Paths.get(mappingSource));
			Model sourceModel = this.modelCache.getModel(mappingSourcePath);

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
					containerNodes = (NodeList) xPath.compile(container).evaluate(doc, XPathConstants.NODESET);

					// Create new container nodes if XPath doesn't return any
					if (containerNodes == null || containerNodes.getLength() == 0) {
						containerNodes = this.createContainerStructure(container);
					}
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				String snippet = mappingDefinition.getSnippet();
				List<String> completedSnippets = this.fillPlaceholders(snippet, results);

				// Add completed snippets to containers
				for (int i = 0; i < containerNodes.getLength(); i++) {
					Node containerNode = containerNodes.item(i);
					for (String completedSnippet : completedSnippets) {
						Document tempDoc = docBuilder.parse(new ByteArrayInputStream(completedSnippet.getBytes()));
						Node snippetNode = doc.importNode(tempDoc.getDocumentElement(), true);
						containerNode.appendChild(snippetNode);
					}
					System.out.println("Container:" + container);
					System.out.println("Adding containerNode: " + containerNode.getNodeName());
					doc.appendChild(containerNode);
				}
			
			}

		}

		return doc;
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
		
		if (allMatches.size() == 0) {
			// If there are no placeholders, return the input string in a list
			completedStrings.add(stringWithPlaceholder);
		} else {
			// if there are placeholders, replace them with the results
			for (QuerySolution result : sparqlResults) {
				String completedString = stringWithPlaceholder;
				for (String match : allMatches) {
					String varName = match.substring(3, match.length() - 1);
					completedString = completedString.replace(match, result.get(varName).toString());
				}
				completedStrings.add(completedString);
			}
		}
		return completedStrings;
	}


	private NodeList createContainerStructure(String container) {
		// First RegEx finds all slash-separated sub-paths of an XPath. These sub-paths might contain attribute conditions (e.g. [@name="asd"])
		Pattern pattern = Pattern.compile("\\/(\\w-*)+(\\[.+\\])?");
		Matcher matcher = pattern.matcher(container);

		Node rootNode = this.doc.createElement("tempRootElememt");
		Node parentNode = rootNode;
		while (matcher.find()) {
			matcher.group();
			String elementName = matcher.group().substring(1); // Removes first slash
			// find complete brackets (\[.+\])?
			// separate attributes.
			// Assumption: Three groups are found
			// 1: Complete bracket: for example "[@id = asd]"
			// 2: Attribute only: in the above example: "id"
			// 3: Value only: in the above example "asd"

			// Second RegEx: Within the current sub-path, find only the atttribute condition (e.g. [@name="asd"])
			List<String> attributeMatches = this.findAllRegexMatches(elementName, "(\\[@(\\w+)=(.*)\\])");

			// Create a new node. If there are attributes, create a node with attributes
			Node node;
			if (attributeMatches.size() > 0) {
				elementName = elementName.substring(0, elementName.length() - attributeMatches.get(1).length());

				String attributeName = attributeMatches.get(2);
				String attributeValue = attributeMatches.get(3);
				node = this.createNodeWithAttribute(elementName, attributeName, attributeValue);
			} else {
				node = this.createNodeWithAttribute(elementName, null, null);
			}

			parentNode.appendChild(node);
			parentNode = node;

		}
		// Return the complete structure (without the placeholder root node)
		NodeList finalNode = rootNode.getChildNodes();
		return finalNode;
	}

	private Node createNodeWithAttribute(String elementName, String attributeName, String attributeValue) {
		Node node = this.doc.createElement(elementName);
		if (attributeName != null) {
			((Element) node).setAttribute(attributeName, attributeValue);
		}
		return node;
	}

	/**
	 * Returns all matches within all groups as a flat list
	 * 
	 * @param stringToSearch
	 * @param patternString
	 * @return
	 */
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
