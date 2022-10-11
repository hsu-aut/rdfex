package olif;

import java.nio.file.Path;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
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
			instance = new ModelCache();
		}
		return instance;
	}

	public Model getModel(Path modelPath) {
		Model cachedModel = this.modelCache.get(modelPath);

		// If this model has been loaded before: Get it from the cache
		if (cachedModel != null) {
			return cachedModel;
		}
		
		// If this modelPath has not been loaded yet: Add it to the cache and return it
		Model model = RDFDataMgr.loadModel(modelPath.toString());
		this.modelCache.put(modelPath, model);
		return model;
	}
	
	public void clear() {
		this.modelCache.clear();
	}

}
