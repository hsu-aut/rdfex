package olif.json;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import olif.MappingResult;

public class JsonMappingResult extends MappingResult {

	JsonElement json;

	public JsonMappingResult(Path outputPath) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		Path outputPathWithoutExtension = this.removeFileExtension(outputPath);
		this.outputPath = outputPathWithoutExtension.resolveSibling(outputPathWithoutExtension.getFileName() + ".json");
		Gson gson = new Gson();
		this.json = gson.fromJson(new FileReader(this.outputPath.toFile()), JsonElement.class);
	}

	@Override
	public void writeResult() {
		// TODO Auto-generated method stub

	}

	JsonElement getRootElement() {
		return this.json;
	}

	void addToElement(JsonElement existingElement, JsonElement elementToAdd) {
		// Primitive can only be added to array
		if (elementToAdd.isJsonPrimitive()) {
			existingElement.getAsJsonArray().add(elementToAdd);
			return;
		}

		// Array can only be added by appending array entries to an existing array
		if (elementToAdd.isJsonArray()) {
			List<JsonElement> arrayElements = elementToAdd.getAsJsonArray().asList();
			for (JsonElement arrayElement : arrayElements) {
				existingElement.getAsJsonArray().add(arrayElement);
			}
			return;
		}

		// Object needs to be added property by property if existing JSON is an object. If it's an arry, it can simply be added
		if (elementToAdd.isJsonObject()) {
			Set<String> keySet = elementToAdd.getAsJsonObject().keySet();
			if(existingElement.isJsonObject()) {
				for (String key : keySet) {
					existingElement.getAsJsonObject().add(key, elementToAdd.getAsJsonObject().get(key));
				}
			}
			if(existingElement.isJsonArray()) {
				existingElement.getAsJsonArray().add(elementToAdd);
			}
			return;
		}
	}

	@Override
	public String getStringResult() {
		String res = json.toString();
		return res;
	}

}
