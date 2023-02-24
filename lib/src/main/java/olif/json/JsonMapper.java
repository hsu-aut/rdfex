package olif.json;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QuerySolution;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import olif.DataMap;
import olif.Mapper;
import olif.MappingResult;
import olif.SourceModel;

public class JsonMapper extends Mapper {

	// static so that all instances of XmlMapper use this result
	protected static JsonMappingResult mappingResult;
	JsonPathReader pathReader;

	@Override
	public void map(DataMap mappingDefinition, Path outputPath) {

		if (mappingResult == null) {
			try {
				mappingResult = new JsonMappingResult(outputPath);
			} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
		}

		// fire SPARQL query
		// TODO: Add all prefixes from mapping document into query header
		SourceModel sourceModel = mappingDefinition.getSourceModel();
		String queryString = mappingDefinition.getQuery();
		List<QuerySolution> results = sourceModel.queryModel(queryString);

		for (QuerySolution result : results) {
			// execute XPath to get the container (with placeholders) and fill possible placeholders
			String containerTemplate = mappingDefinition.getContainer();
			String container = this.fillPlaceholder(containerTemplate, result);

			// Get all possible container nodes
			this.pathReader = new JsonPathReader();
			this.findOrCreateContainer(container);
			List<JsonElement> containerElements = null;
			// TODO: This is awkward, same query is made in the method, should only be there. Move it (also for XML)
			containerElements = this.pathReader.read(mappingResult.json, container);
			
			String snippet = mappingDefinition.getSnippet();
			String completedSnippet = this.fillPlaceholder(snippet, result);
			
			Gson gson = new Gson();
			JsonObject snippetElement = gson.fromJson(completedSnippet, JsonObject.class);
//			mappingResult.addToElement(containerElements, snippetElement);
			for (JsonElement containerElement : containerElements) {
				
				String containerBeforeChange = containerElement.toString();
				mappingResult.addToElement(containerElement, snippetElement);
				String containerAfterChange = containerElement.toString();
				
				String replacedResult = mappingResult.json.toString().replace(containerBeforeChange, containerAfterChange);
				mappingResult.json = JsonParser.parseString(replacedResult);

			}
		}

	}

	private List<JsonElement> findOrCreateContainer(String container) {
		// If the container exists, get it via JsonPath and add it to the list
		List<JsonElement> containerNodes = this.pathReader.read(mappingResult.json, container);

		// If the container doesn't exist: create new container node, add it to the list and to the document
		if (containerNodes == null || containerNodes.size() == 0) {
			JsonElement createdContainerElement = this.createContainerStructure(container);
			for (JsonElement containerNode : containerNodes) {
				mappingResult.addToElement(containerNode, createdContainerElement);
			}
//			mappingResult.addToElement(mappingResult.getRootElement(), createdContainerElement);
		}

		return containerNodes;
	}
	
	
	private JsonElement createContainerStructure(String container) {
		// First RegEx finds all slash-separated sub-paths of an XPath. These sub-paths might contain attribute conditions (e.g. [@name="asd"])
		Pattern pattern = Pattern.compile("(?:\\['(\\w+)'\\]|\\.(\\w+))(\\[\\d+\\])*(\\[\\?\\(@\\.\\w+(?:\\s*[<=>]\\s*\\w+)?\\)\\])?");
		Matcher matcher = pattern.matcher(container);
		
//		Node rootNode = mappingResult.doc.createElement("tempRootElememt");
		JsonElement parentElement = mappingResult.getRootElement();
		while (matcher.find()) {
			String elementName;
			try {
				elementName = matcher.group(0);
			} catch (Exception e) {
				elementName = matcher.group(1);
			}
			
			JsonElement existingElement = this.pathReader.read(mappingResult.json, elementName).get(0);
			JsonElement element;
			if(existingElement != null) {
				element = existingElement;
			} else {
				String attributeMatches = matcher.group(3);		// Filter condition is in group 3

				// Create a new node. If there are attributes, create a node with attributes
				if (!attributeMatches.isEmpty()) {
					String attributeName = matcher.group(4);
					String attributeValue = matcher.group(5);
					element = new JsonObject();
					element.getAsJsonObject().addProperty(attributeName, attributeValue);
				} else {
					element = new JsonObject();
				}
			}
			mappingResult.addToElement(parentElement, element);
			parentElement = element;

		}
		JsonElement result = mappingResult.getRootElement();
		return result;
	}


	@Override
	public MappingResult getResult() {
		return mappingResult;
	}
	
	protected void clearResult() {
		mappingResult = null;
	}

}
