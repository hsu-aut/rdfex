package olif;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QuerySolution;

/**
 * A class representing DataMap individuals (i.e. individual mappings) within a mapping document.
 */
public class DataMap {
	private SourceModel sourceModel;
	private String queryLanguage;
	private String query;
	private String targetFormat;
	private String container;
	private String snippet;
	
	public DataMap(QuerySolution querySolution, Path mappingFilePath) {
		// Get the source model
		String sourceType = querySolution.getResource("sourceType").toString();
		String source = querySolution.getLiteral("source").toString();
		Path mappingFileDirectory = mappingFilePath.getParent();
		this.sourceModel = SourceModelFactory.getModel(sourceType, source, mappingFileDirectory);	// Directory is needed for file models
		
		this.queryLanguage = querySolution.getResource("queryLanguage").toString();
		this.query = querySolution.getLiteral("query").toString();
		this.targetFormat = querySolution.getResource("targetFormat").toString();
		this.container = querySolution.getLiteral("container").toString();
		this.snippet = querySolution.getLiteral("snippet").getString();
	}
	
	
	public SourceModel getSourceModel() {
		return this.sourceModel;
	}

	public String getQueryLanguage() {
		return this.queryLanguage;
	}

	public String getQuery() {
		return this.query;
	}

	public String getTargetFormat() {
		return this.targetFormat;
	}

	public String getContainer() {
		return this.container;
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
		return this.snippet;
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


