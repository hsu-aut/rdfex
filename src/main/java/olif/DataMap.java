package olif;

import org.apache.jena.query.QuerySolution;

/**
 * A class representing DataMap individuals within a mapping document.
 */
public class DataMap {
	private String source;
	private String sourceType;
	private String queryLanguage;
	private String query;
	private String targetFormat;
	private String container;
	private String snippet;
	
	public DataMap(QuerySolution querySolution) {
		this.source = querySolution.getLiteral("source").toString();
		this.sourceType = querySolution.getResource("sourceType").toString();
		this.queryLanguage = querySolution.getResource("queryLanguage").toString();
		this.query = querySolution.getLiteral("query").toString();
		this.targetFormat = querySolution.getResource("targetFormat").toString();
		this.container = querySolution.getLiteral("container").toString();
		this.snippet = querySolution.getLiteral("snippet").toString();
	}

	public String getSource() {
		return source;
	}

	public String getSourceType() {
		return sourceType;
	}

	public String getQueryLanguage() {
		return queryLanguage;
	}

	public String getQuery() {
		return query;
	}

	public String getTargetFormat() {
		return targetFormat;
	}

	public String getContainer() {
		return container;
	}

	public String getSnippet() {
		return snippet;
	}
	
}
