package olif;

import java.util.List;

import org.apache.jena.query.QuerySolution;

public interface SourceModel {

	List<QuerySolution> queryModel(String queryString);
	
}
