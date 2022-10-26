package olif;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QuerySolution;


public abstract class Mapper {
	
	// Model cache to store models once they're loaded to get them faster for subsequent uses of the same model
	protected ModelCache modelCache = ModelCache.getInstance();
	
	/**
	 * Actual transformation method that converts elements from the source according to a mappingDefinition in order to (later) write them to an output path
	 * @param mappingDefinition
	 * @param mappingSourcePath
	 * @param outputFilePath
	 */
	public abstract void map(DataMap mappingDefinition, Path outputFilePath);
	
	public abstract MappingResult getResult();
	

	/**
	 * Returns all matches within all groups as a flat list
	 * 
	 * @param stringToSearch
	 * @param patternString
	 * @return
	 */
	protected List<String> findAllRegexMatches(String stringToSearch, String patternString) {
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(stringToSearch);
		List<String> allMatches = new ArrayList<String>();
		while (matcher.find()) {
			for (int i = 0; i <= matcher.groupCount(); i++) {
				allMatches.add(matcher.group(i));
			}
		}
		return allMatches;
	}
	
	
	/**
	 * Take a string containing ${?...} placeholders and fill it with sparql result
	 * 
	 * @param stringWithPlaceholder
	 * @param sparqlResult
	 * @return
	 */
	protected String fillPlaceholder(String stringWithPlaceholder, QuerySolution sparqlResult) {
		// Get all template strings
		List<String> allMatches = this.findAllRegexMatches(stringWithPlaceholder, "(\\$\\{\\?\\w*\\})");
		String completedString = stringWithPlaceholder;
		
		// Only if there are variables, a replacement is necessary. Otherwise just return the input string.
		if (allMatches.size() > 0) {
			// if there are placeholders, replace them with values from the result
			for (String match : allMatches) {
				String varName = match.substring(3, match.length() - 1);
				completedString = completedString.replace(match, sparqlResult.get(varName).toString());
			}
		}
		return completedString;
	}
	
	
	
	@Override
	public int hashCode() {
		return this.getClass().getName().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Mapper && (this.getClass().getName() == obj.getClass().getName()));
	}
}
