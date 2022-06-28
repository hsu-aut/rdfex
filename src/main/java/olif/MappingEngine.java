package olif;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import olif.DataMap.ContainerVariableCountComparator;

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

		// Sort mapping definitions so that containers with least amount of variables come first. 
		// -> This allows to write mappings that build on top of other ones with a deterministic behavior
		Collections.sort(mappings, new ContainerVariableCountComparator());

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


			for (QuerySolution result : results) {
				// execute XPath to get the container (with placeholders) and fill possible placeholders
				String containerTemplate = mappingDefinition.getContainer();
				String container = this.fillPlaceholder(containerTemplate, result);
				
				// Get all possible container nodes
				List<Node> containerNodes = this.findOrCreateContainer(container);
				
				for (Node containerNode : containerNodes) {
					String snippet = mappingDefinition.getSnippet();
					String completedSnippet = this.fillPlaceholder(snippet, result);
					
					// Add completed snippets to containers
					// The snippet could be a multiple tags next to each other. This would cause problems (multiple root nodes) when parsing the snippet
					// The snippet is therefore padded with a temporary super node to mitigate this issues
					String paddedSnippet = "<tempRoot>" + completedSnippet + "</tempRoot>";
					Document tempDoc = docBuilder.parse(new ByteArrayInputStream(paddedSnippet.getBytes()));
					Node paddedSnippetNode = doc.importNode(tempDoc.getDocumentElement(), true);
					// Add child nodes of the padded snippet (= add the original snippet)
					NodeList paddedSnippetChildren = paddedSnippetNode.getChildNodes(); 
					for (int i = 0; i < paddedSnippetNode.getChildNodes().getLength(); i++) {
						Node snippetNode = paddedSnippetNode.getChildNodes().item(i).cloneNode(true);
						containerNode.appendChild(snippetNode);
					}
				}
			}
			
		}

		return doc;
	}
	
	
	private List<Node> findOrCreateContainer(String container) {
		List<Node> containerNodes = new ArrayList<Node>();
		XPath xPath = XPathFactory.newDefaultInstance().newXPath();
		
		try {
			// If the container exists, get it via XPath and add it to the list
			NodeList containerNodeList = (NodeList)xPath.compile(container).evaluate(doc, XPathConstants.NODESET);
			for (int i = 0; i < containerNodeList.getLength(); i++) {
				containerNodes.add(containerNodeList.item(i));
			}
			
			// If the container doesn't exist: create new container node, add it to the list and to the document
			if (containerNodes == null || containerNodes.size() == 0) {
				Node createdContainerNode = this.createContainerStructure(container);
				containerNodes.add(createdContainerNode);
				doc.appendChild(createdContainerNode);
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return containerNodes;
	}

	/**
	 * Retrieves all mapping definitions inside the current mapping and converts them into an easily accessible object that represents mapping definitions
	 * 
	 * @return All mapping definitions
	 */
	public List<DataMap> getAllMappingDefinitions(Model model) {
		String queryString = "PREFIX ol: <http://www.hsu-hh.de/aut/ontologies/olif#>"
				+ "SELECT ?mappingDefinition ?source ?sourceType ?targetFormat ?queryLanguage ?query ?container ?snippet WHERE {"
				+ "?mappingDefinition ol:ontologicalSource ?ontologicalSource." 
				+ "?ontologicalSource ol:source ?source;" 
				+ "ol:sourceType ?sourceType;"
				+ "ol:queryLanguage ?queryLanguage;" 
				+ "ol:query ?query." 
				+ "?mappingDefinition ol:container ?container;" 
				+ "ol:targetFormat ?targetFormat;"
				+ "ol:snippet ?snippet." + "}";

		Query mappingQuery = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(mappingQuery, model);

		ResultSet results = qexec.execSelect();
		List<QuerySolution> querySolutions = ResultSetFormatter.toList(results);

		List<DataMap> dataMaps = querySolutions.stream().map(qS -> new DataMap(qS)).collect(Collectors.toList());
		return dataMaps;
	}

	/**
	 * Take a string containing ${?...} placeholders and fill it with sparql result
	 * 
	 * @param stringWithPlaceholder
	 * @param sparqlResult
	 * @return
	 */
	public String fillPlaceholder(String stringWithPlaceholder, QuerySolution sparqlResult) {
		// Get all template strings
		List<String> allMatches = this.findAllRegexMatches(stringWithPlaceholder, "(\\$\\{\\?\\w*\\})");
		String completedString = stringWithPlaceholder;
		
		// Only if there are variables, a replacement is necessary. Otherwise just return the input string.
		if (allMatches.size() > 0) {
			// if there are placeholders, replace them with values from the result
			for (String match : allMatches) {
				String varName = match.substring(3, match.length() - 1);
				completedString = completedString.replace(match, sparqlResult.get(varName).toString());
			}
		}
		return completedString;
	}


	private Node createContainerStructure(String container) {
		// First RegEx finds all slash-separated sub-paths of an XPath. These sub-paths might contain attribute conditions (e.g. [@name="asd"])
		Pattern pattern = Pattern.compile("\\/(\\w-*)+(\\[.+\\])?");
		Matcher matcher = pattern.matcher(container);

		Node rootNode = this.doc.createElement("tempRootElememt");
		Node parentNode = rootNode;
		while (matcher.find()) {
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
		Node finalNode = rootNode.getFirstChild();
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
