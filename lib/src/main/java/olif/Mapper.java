package olif;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;

public abstract class Mapper {

	/**
	 * Actual transformation method that converts elements from the source according to a mappingDefinition in order to (later) write them to an output
	 * path
	 * 
	 * @param mappingDefinition
	 * @param mappingSourcePath
	 * @param outputFilePath
	 */
	public abstract void map(DataMap mappingDefinition, Path outputFilePath);

	public abstract MappingResult getResult();

	protected abstract void clearResult();

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

		// if there are placeholders, replace them with values from the result. Otherwise just return the input string.
		for (String match : allMatches) {
			String varName = match.substring(3, match.length() - 1);
			String sparqlResultValue = this.getSparqlResultValue(sparqlResult.get(varName));
			completedString = completedString.replace(match, sparqlResultValue);
		}
		return completedString;
	}

	protected String getSparqlResultValue(RDFNode resultNode) {
		// If its a literal, return value. Else return URI.
		// Todo: What if a user passes in a blank node?
		if (resultNode.isLiteral()) {
			return resultNode.asLiteral().getLexicalForm();
		}

		return resultNode.asNode().getURI();
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
