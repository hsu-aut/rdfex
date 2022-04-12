package owl2xml;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Owl2XmlMapper {

	public Document map(Path path) throws ParserConfigurationException {
		// Create mapping model
		Model mappingModel = this.getModelFromFile(path);

		// Create empty XML file
		DocumentBuilder docBuilder = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		// Get all mapping definitions
		List<QuerySolution> mappings = this.getAllMappingDefinitions(mappingModel);

		// For each mapping:
		for (QuerySolution mappingDefinition : mappings) {
			// open source file
			// TODO (performance optimization) Put models in some kind of cache to not load one model twice
			String mappingSource = mappingDefinition.getLiteral("source").toString();
			Path directory = path.getParent();
			Path mappingSourcePath = directory.resolve(Paths.get(mappingSource));
			// Path mappingSourcePath = Paths.get(directory, mappingSource);
			Model sourceModel = getModelFromFile(mappingSourcePath);

			// fire SPARQL query
			// TODO: Add all prefixes from mapping document into query header
			String queryString = mappingDefinition.getLiteral("query").toString();
			Query mappingQuery = QueryFactory.create(queryString);
			QueryExecution qexec = QueryExecutionFactory.create(mappingQuery, sourceModel);

			ResultSet resultSet = qexec.execSelect();
			List<QuerySolution> results = ResultSetFormatter.toList(resultSet);

			// execute XPath to get the container (with placeholders)
			String containerString = mappingDefinition.getLiteral("container").toString();
			// fill possible placeholders
			List<String> containers = this.fillPlaceholders(containerString, results);
			
			for (String container : containers) {
				XPath xPath = XPathFactory.newDefaultInstance().newXPath();
				
				NodeList containerNodes = null;
				try {
					containerNodes = (NodeList) xPath.compile(containerString).evaluate(doc, XPathConstants.NODESET);
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					// TODO: Implement creating new container nodes
					System.out.println("The container node doesn't exist. Creating new container nodes is not implemented yet.");
					e.printStackTrace();
				}
				
				
				String snippet = mappingDefinition.getLiteral("snippet").toString();
				List<String> completedSnippets = this.fillPlaceholders(snippet, results);
				
				// for each container result
				for (int i = 0; i < containerNodes.getLength(); i++) {
					Node containerNode = containerNodes.item(i);
					
					// add enriched snippet to XML
					for (String completedSnippet : completedSnippets) {
						containerNode.setTextContent(completedSnippet);
						doc.appendChild(containerNode);
					}
					
				}
				
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
	 * Retrieves all mapping definitions inside the current mapping
	 * 
	 * @return All mapping definitions
	 */
	public List<QuerySolution> getAllMappingDefinitions(Model model) {
		String queryString = "PREFIX rml: <http://semweb.mmlab.be/ns/rml#>"
				+ "SELECT ?mappingDefinition ?source ?query ?container ?snippet WHERE {"
				+ "?mappingDefinition rml:logicalSource ?logicalSource." 
				+ "?logicalSource rml:source ?source;" 
				+ "rml:query ?query." 
				+ "?mappingDefinition rml:container ?container;"
				+ "rml:snippet ?snippet." 
				+ "}";

		Query mappingQuery = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(mappingQuery, model);

		ResultSet results = qexec.execSelect();
		return ResultSetFormatter.toList(results);
	}
	
	
	/**
	 * Take a string containing ${?...} placeholders and fill it with sparql results
	 * @param stringWithPlaceholder
	 * @param sparqlResults
	 * @return
	 */
	public List<String> fillPlaceholders(String stringWithPlaceholder, List<QuerySolution> sparqlResults) {
		// Get all template strings
		Pattern pattern = Pattern.compile("(\\$\\{\\?\\w*\\})");
		Matcher matcher = pattern.matcher(stringWithPlaceholder);
		List<String> allMatches = new ArrayList<String>();
		while (matcher.find()) {
			allMatches.add(matcher.group());
		}
		
		List<String> completedStrings = new ArrayList<String>();
		for (QuerySolution result : sparqlResults) {
			String completedString = stringWithPlaceholder;
			for (String match : allMatches) {
				String varName = match.substring(3, match.length()-1);
				completedString = completedString.replace(match, result.get(varName).toString());
			}			
			completedStrings.add(completedString);
		}
		return completedStrings;
	}

}
