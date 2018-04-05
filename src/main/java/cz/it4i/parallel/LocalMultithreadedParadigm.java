package cz.it4i.parallel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = ParallelizationParadigm.class)
public class LocalMultithreadedParadigm extends SimpleOstravaParadigm {

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.LocalMultithreadedParadigm.class);

	@Override
	public void init() {
		if (poolSize == null) {
			poolSize = 1;
		}
		super.init();
	}

	@Override
	protected void initWorkerPool() {
		for (int i = 0; i < poolSize; i++) {
			workerPool.addWorker(new LocalMultithreadedPluginWorker());
		}
	}

	@Override
	protected void setValue(ParallelWorker worker,Map<String, Object> args, String executeResult, String propertyName, Object object) {
		if (object instanceof Path) {
			Path path = (Path) object;
			String ret = worker.uploadFile(path.toAbsolutePath().toString(), getFileType(path),
					path.getFileName().toString());
			object = ret;
		}
		args.put(propertyName, object);
	}

	@Override
	protected Object getValue(ParallelWorker worker, String executeResult, String propertyName, Class<?> returnType) {
		String resultValue = executeResult.toString();
		// download png image given by id of result
		Object result;
		if (returnType.equals(Path.class)) {
			Path p = Paths.get("/tmp/output/" + resultValue + ".png");
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
