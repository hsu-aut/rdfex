package olif.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import olif.DataMap;
import olif.Mapper;

public class XmlMapper extends Mapper {


	@Override
	public void map(DataMap mappingDefinition, Path mappingSourcePath, Path outputPath) {
		// Get source model
		Model sourceModel = this.modelCache.getModel(mappingSourcePath);

		if (mappingResult == null ) {
			mappingResult = new XmlMappingResult(outputPath);
		}
		
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
				// The snippet could be a multiple tags next to each other. This would cause problems (multiple root nodes) when parsing the snippet into an empty
				// temp doc. The snippet is therefore padded with a temporary super node to mitigate this issues
				String paddedSnippet = "<tempRoot>" + completedSnippet + "</tempRoot>";
				Document tempDoc = null;
				try {
					DocumentBuilder docBuilder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();
					tempDoc = docBuilder.parse(new ByteArrayInputStream(paddedSnippet.getBytes()));
				} catch (SAXException | IOException | ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Node paddedSnippetNode = mappingResult.getDocument().importNode(tempDoc.getDocumentElement(), true);
				// Add child nodes of the padded snippet (= add the original snippet)
				for (int i = 0; i < paddedSnippetNode.getChildNodes().getLength(); i++) {
					Node snippetNode = paddedSnippetNode.getChildNodes().item(i).cloneNode(true);
					containerNode.appendChild(snippetNode);
				}
			}
		}
	}

	

	
	
	private Node createContainerStructure(String container) {
		// First RegEx finds all slash-separated sub-paths of an XPath. These sub-paths might contain attribute conditions (e.g. [@name="asd"])
		Pattern pattern = Pattern.compile("\\/(\\w-*)+(\\[.+\\])?");
		Matcher matcher = pattern.matcher(container);

		Node rootNode = mappingResult.doc.createElement("tempRootElememt");
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
		Node node = mappingResult.getDocument().createElement(elementName);
		if (attributeName != null) {
			((Element) node).setAttribute(attributeName, attributeValue);
		}
		return node;
	}

	private List<Node> findOrCreateContainer(String container) {
		List<Node> containerNodes = new ArrayList<Node>();
		XPath xPath = XPathFactory.newDefaultInstance().newXPath();

		try {
			// If the container exists, get it via XPath and add it to the list
			NodeList containerNodeList = (NodeList) xPath.compile(container).evaluate(mappingResult.getDocument(), XPathConstants.NODESET);
			for (int i = 0; i < containerNodeList.getLength(); i++) {
				containerNodes.add(containerNodeList.item(i));
			}

			// If the container doesn't exist: create new container node, add it to the list and to the document
			if (containerNodes == null || containerNodes.size() == 0) {
				Node createdContainerNode = this.createContainerStructure(container);
				containerNodes.add(createdContainerNode);
				mappingResult.getDocument().appendChild(createdContainerNode);
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return containerNodes;
	}
	

}