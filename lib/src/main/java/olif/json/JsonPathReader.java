package olif.json;


import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;

public class JsonPathReader {

	Configuration config;
	Gson gson = new Gson();
	
	public JsonPathReader() {
		this.config = new Configuration.ConfigurationBuilder()
				.jsonProvider(new GsonJsonProvider())
				.options(Option.ALWAYS_RETURN_LIST)
				.mappingProvider(new GsonMappingProvider())
				.build();
	}
	
	
	List<JsonElement> read(JsonElement json, String pathToRead) {
		List<Object> preResults = JsonPath
                .using(this.config)
                .parse(json)
                .read(pathToRead, List.class);
		
		List<JsonElement> results = preResults.stream().map(res -> gson.toJsonTree(res)).collect(Collectors.toList());
		
		return results;
	}
}
