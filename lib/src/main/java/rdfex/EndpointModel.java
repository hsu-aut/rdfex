package rdfex;

import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;

/**
 * Class that wraps access to a model contained in a SPARQL endpoint.
 */
public class EndpointModel implements SourceModel {

	RDFConnection connection;
	
	/**
	 * Sets up the connection to a SPARQL endpoint with a given URL
	 * @param endpointUrl URL of the endpoint to connect to
	 */
	public EndpointModel(String endpointUrl) {
		this.connection = RDFConnection.connect(endpointUrl);
	}
	
	@Override
	public List<QuerySolution> queryModel(String queryString) {
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = connection.query(query);
		ResultSet resultSet = queryExecution.execSelect();
		List<QuerySolution> querySolutions = ResultSetFormatter.toList(resultSet);
		return querySolutions;
	}

}
