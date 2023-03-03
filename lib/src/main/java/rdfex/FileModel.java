package rdfex;

import java.nio.file.Path;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;

public class FileModel implements SourceModel {

	ModelCache modelCache = ModelCache.getInstance();
	Model model;
	
	public FileModel(Path filePath) {
		this.model = this.modelCache.getModel(filePath);
	}
	
	@Override
	public List<QuerySolution> queryModel(String queryString) {
		// TODO Auto-generated method stub
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, this.model);
		ResultSet resultSet = queryExecution.execSelect();
		List<QuerySolution> querySolutions = ResultSetFormatter.toList(resultSet);
		return querySolutions;
	}

}
