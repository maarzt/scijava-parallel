package cz.it4i.parallel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Plugin(type = ParallelizationParadigm.class)
public class ImageJServerParadigm extends SimpleOstravaParadigm {

	// private static final String ADDRESS = "address";
	private final static Set<String> supportedImageTypes = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList("png", "jpg")));

	private static final String PORT = "port";


	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.ImageJServerParadigm.class);

	private final Collection<String> hosts = new LinkedList<>();
	private final Map<String,String> id2FileType = new HashMap<>();

	@Override
	public void init() {
		if (poolSize == null) {
			poolSize = Math.max(hosts.size(), 1);
		}
		// Unused persistence code, to be revived later
		retrieveConnectionConfig();
		if (connectionConfig.size() == 0) {
			Map<String, String> configEntries = new LinkedHashMap<>();
			//configEntries.put(ADDRESS, "localhost");
			configEntries.put(PORT, "8080");
			updateConnectionConfig(configEntries);
		} 
		super.init();
	}

	public void setHosts(Collection<String> hosts) {
		this.hosts.clear();
		this.hosts.addAll(hosts);

	}

	@Override
	protected void initWorkerPool() {
		hosts.forEach(host -> workerPool
				.addWorker(new ImageJServerWorker(host, Integer.parseInt(connectionConfig.get(PORT)))));

	}
	
	@Override
	protected void setValue(ParallelWorker worker,Map<String, Object> args, String executeResult, String propertyName, Object object) {
		if (object instanceof Path) {
			Path path = (Path) object;
			String fileType = getFileType(path);
			String mimeType = "image/" + fileType;
			String ret = worker.uploadFile(path.toAbsolutePath().toString(), mimeType,
					path.getFileName().toString());
			// obtain uploaded file id
			object = new org.json.JSONObject(ret).getString("id");
			id2FileType.put((String) object, fileType);
			
		}
		args.put(propertyName, object);

	}
	
	@Override
	protected Object getValue(ParallelWorker worker, String executeResult, String propertyName, Class<?> returnType) {
		String resultValue = (new JSONObject(executeResult)).getString(propertyName);
		// download png image given by id of result
		Object result;
		if (returnType.equals(Path.class)) {
			Path p = Paths.get("/tmp/output/" + resultValue.split(":")[1] + "." + id2FileType.get(resultValue));
			worker.downloadFile(resultValue, p.toString(), id2FileType.get(resultValue));
			worker.deleteResource(resultValue);
			id2FileType.remove(resultValue);
			result = p;
		} else {
			result = resultValue;
		}
		return result;
	}

	// TODO: support another types
	private String getFileType(Path path) {
		for(String type: supportedImageTypes) {
			if (path.toString().endsWith("."+type)) {
				return type;
			}
		}
		
		throw new UnsupportedOperationException("Only " + supportedImageTypes +  " image files supported");
	}
}
