package cz.it4i.parallel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONObject;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = ParallelizationParadigm.class)
public class ImageJServerParadigm extends SimpleOstravaParadigm {

	// private static final String ADDRESS = "address";
	private static final Integer PORT = 8080;

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.ImageJServerParadigm.class);

	private final Collection<String> hosts = new LinkedList<>();

	@Override
	public void init() {
		if (poolSize == null) {
			poolSize = Math.max(hosts.size(), 1);
		}
		super.init();
		
		// Unused persistence code, to be revived later
		/* retrieveConnectionConfig();
		if (connectionConfig.size() == 0) {
			Map<String, String> configEntries = new LinkedHashMap<>();
			configEntries.put(ADDRESS, "localhost");
			configEntries.put(PORT, "8080");
			updateConnectionConfig(configEntries);
		} */
	}

	public void setHosts(Collection<String> hosts) {
		this.hosts.clear();
		this.hosts.addAll(hosts);

	}

	@Override
	protected void initWorkerPool() {
		hosts.forEach(host -> workerPool
				.addWorker(new ImageJServerWorker(host, PORT)));

	}
	
	@Override
	protected void setValue(ParallelWorker worker,Map<String, Object> args, String executeResult, String propertyName, Object object) {
		if (object instanceof Path) {
			Path path = (Path) object;
			String ret = worker.uploadFile(path.toAbsolutePath().toString(), getFileType(path),
					path.getFileName().toString());
			// obtain uploaded file id
			object = new org.json.JSONObject(ret).getString("id");
		}
		args.put(propertyName, object);

	}
	
	@Override
	protected Object getValue(ParallelWorker worker, String executeResult, String propertyName, Class<?> returnType) {
		String resultValue = (new JSONObject(executeResult)).getString(propertyName);
		// download png image given by id of result
		Object result;
		if (returnType.equals(Path.class)) {
			Path p = Paths.get("/tmp/output/" + resultValue.split(":")[1] + ".png");
			worker.downloadFile(resultValue, p.toString(), "png");
			worker.deleteResource(resultValue);
			result = p;
		} else {
			result = resultValue;
		}
		return result;
	}

	// TODO: support another types
	private String getFileType(Path path) {
		if (!path.toString().endsWith(".png")) {
			throw new UnsupportedOperationException("Only png files supported");
		}
		return "image/png";
	}
}
