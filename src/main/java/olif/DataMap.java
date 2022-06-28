package olif;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QuerySolution;
import org.w3c.dom.Node;

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
	
	public List<String> getContainerVariables() {
		Pattern pattern = Pattern.compile("\\$\\{\\?[\\w-]+\\}");
		Matcher matcher = pattern.matcher(this.container);
		List<String> containerVariables = new ArrayList<String>();
		while (matcher.find()) {
			containerVariables.add(matcher.group());
		}
		return containerVariables;
	}

	public String getSnippet() {
		return snippet;
	}
	
	public static class ContainerVariableCountComparator implements Comparator<DataMap>
	{
	    public int compare(DataMap m1, DataMap m2)
	    {
	        Integer m1NumberOfVariables = m1.getContainerVariables().size();
	        Integer m2NumberOfVariables = m2.getContainerVariables().size(); 
	    	return m1NumberOfVariables.compareTo(m2NumberOfVariables);
	    }
	}
	
}


