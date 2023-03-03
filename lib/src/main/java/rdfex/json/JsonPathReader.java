package rdfex.json;


import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

public class JsonPathReader {

	Configuration config;
	Gson gson = new Gson();
	
	public JsonPathReader() {
		// Note that Gson can not be used as the JsonProvider and MappingProvider as it adds trailing ".0" to integers
		// Maybe this can be changed by passing a pre-configured Gson object to GsonJsonProdiver?
		this.config = new Configuration.ConfigurationBuilder()
				.jsonProvider(new JacksonJsonProvider())
				.options(Option.ALWAYS_RETURN_LIST)
				.mappingProvider(new JacksonMappingProvider())
				.build();
	}
	
	
	List<JsonElement> read(JsonElement json, String pathToRead) {
		TypeRef<List<Object>> typeRef = new TypeRef<List<Object>>() {};
		List<Object> preResults = JsonPath
                .using(this.config)
                .parse(json.toString())
                .read(pathToRead, typeRef);
		
		List<JsonElement> results = preResults.stream().map(res -> gson.toJsonTree(res)).collect(Collectors.toList());
		
		return results;
	}
	
}
