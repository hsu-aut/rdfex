package olif;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;

import olif.DataMap.ContainerVariableCountComparator;

public class MappingEngine {

	// Model cache to take care of loading models
	ModelCache modelCache = ModelCache.getInstance();
	Set<Mapper> mappers = new HashSet<Mapper>();

	Path mappingFilePath; 
	
	public MappingEngine(Path mappingFilePath) {
		this.mappingFilePath = mappingFilePath;
	}
	
	public List<MappingResult> map(String outputPathString) {
		Path outputPath = this.mappingFilePath.getParent().resolve(outputPathString);
		return this.map(outputPath);
	}
	
	public List<MappingResult> map(Path outputPath) {
		this.modelCache.clear();
		// Create mapping model from the mapping file
		Model mappingModel = this.modelCache.getModel(this.mappingFilePath);
		
		// Get all mapping definitions
		List<DataMap> mappings = this.getAllMappingDefinitions(mappingModel);

		// Sort mapping definitions so that containers with least amount of variables come first. 
		// -> This allows to write mappings that build on top of other ones with a deterministic behavior
		Collections.sort(mappings, new ContainerVariableCountComparator());

		for (DataMap mappingDefinition : mappings) {
			
			// Get the appropriate mapper for each mapping and execute the mapping
			String targetFormat = mappingDefinition.getTargetFormat();
			Mapper mapper = MapperFactory.getMapper(targetFormat);
			this.mappers.add(mapper);

			mapper.map(mappingDefinition, outputPath);
			
		}

		// After all mappings are done: Collect the results and return
		List<MappingResult> mappingResults = new ArrayList<MappingResult>();
		for (Mapper mapper : this.mappers) {
			mappingResults.add(mapper.getResult());
		}
		return mappingResults;
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

		List<DataMap> dataMaps = querySolutions.stream().map(qS -> new DataMap(qS, this.mappingFilePath)).collect(Collectors.toList());
		return dataMaps;
	}
	
}
