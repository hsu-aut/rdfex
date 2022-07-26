package olif;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

/**
 * A simple storage to store models with their path as a key so that models that need to be used multiple times don't have to be deserialized more
 * than once.
 */
public class ModelCache {

	// Cache is a singleton
	private static ModelCache instance;

	// Cache is implemented as a simple hashmap
	private HashMap<Path, Model> modelCache = new HashMap<>();

	// Singleton -> private constructor
	private ModelCache() {
	}

	public static ModelCache getInstance() {
		if (instance == null) {
			return new ModelCache();
		}
		return instance;
	}

	Model getModel(Path modelPath) {
		Model cachedModel = this.modelCache.get(modelPath);

		// If this model has been loaded before: Get it from the cache
		if (cachedModel != null) {
			return cachedModel;
		}
		
		// If this modelPath has not been loaded yet: Add it to the cache and return it
		Model model = getModelFromFile(modelPath);
		this.modelCache.put(modelPath, model);
		return model;
	}

	/**
	 * Create a Jena model from the given mapping Turtle file
	 * 
	 * @param path Path to the mapping definition
	 * @return Jena model of the mapping definition
	 */
	private Model getModelFromFile(Path path) {
		// Create a model from the mapping file
		InputStream in = RDFDataMgr.open(path.toString());
		if (in == null) {
			throw new IllegalArgumentException("File: " + path + " not found");
		}

		Model model = ModelFactory.createDefaultModel();
		// read the file
		// TODO: Get the real file format
		model.read(in, null, "TTL");
		return model;
	}

}
