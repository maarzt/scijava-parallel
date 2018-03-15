package cz.it4i.parallel;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

import org.json.JSONObject;
import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.ParallelTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleOstravaParadigm extends AbstractParallelizationParadigm {

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.SimpleOstravaParadigm.class);

	protected Integer poolSize;

	protected WorkerPool workerPool;

	private ForkJoinPool pool;

	@Override
	public void init() {
		workerPool = new WorkerPool();
		initWorkerPool();
		if (pool != null) {
			pool.shutdown();
		}
		pool = new ForkJoinPool(poolSize);
		
	}

	abstract protected void initWorkerPool();

	@Override
	public void submit() {
		// TODO Auto-generated method stub, consider moving to
		// AbstractParallelizationParadigm

	}

	@Override
	public <T> void parallelLoop(Iterable<T> arguments, BiConsumer<T, ParallelTask> consumer) {
		pool.submit(() -> StreamSupport.stream(arguments.spliterator(), true).forEach(
				val -> {
			try (P_ParallelTask task = new P_ParallelTask()) {
				consumer.accept(val, task);
			}}
		)).join();
	}

	private class P_ParallelTask implements ParallelTask, Closeable {

		private ParallelWorker worker;

		private String executeResult;

		public P_ParallelTask() {
			try {
				worker = workerPool.takeFreeWorker();
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getRemoteModule(Class<T> type) {
			return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] { type },
					new P_InvocationHandler(type));
		}

		@Override
		public void close() {
			workerPool.addWorker(worker);
		}

		@Override
		public <T> void run(Class<T> type, Map<String, Object> inputMap) {
			executeResult = worker.executeModule("command:" + type, inputMap);

		}

		private class P_InvocationHandler implements InvocationHandler {

			private Map<String, Object> args = new HashMap<>();
			private String typeName;
			private Set<String> uploadedValues = new HashSet<>();

			public P_InvocationHandler(Class<?> type) {
				this.typeName = getTypeName(type);
			}

			// TODO make better connection to real type - e.g. based on annotations
			private String getTypeName(Class<?> type) {
				return type.getPackage().getName() + "." + type.getSimpleName().substring(1);
			}

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName().startsWith("set")) {
					setValue(getPropertyName(method.getName()), args[0]);
				} else if (method.getName().equals("run")) {
					// execute worker
					executeResult = worker.executeModule("command:" + typeName, this.args);
					cleanUploadedFiles();
				} else if (method.getName().startsWith("get")) {
					return getValue(getPropertyName(method.getName()), method.getReturnType());
				}
				return null;
			}

			private void cleanUploadedFiles() {
				uploadedValues.forEach(id -> worker.deleteResource(id));
				uploadedValues.clear();

			}

			private Object getValue(String propertyName, Class<?> returnType) {

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

			// TODO: make conversion more flexible
			private void setValue(String propertyName, Object object) {
				if (object instanceof Path) {
					Path path = (Path) object;
					String ret = worker.uploadFile(path.toAbsolutePath().toString(), getFileType(path),
							path.getFileName().toString());
					// obtain uploaded file id
					object = new org.json.JSONObject(ret).getString("id");
					uploadedValues.add((String) object);
				}
				args.put(propertyName, object);
			}

			// TODO: support another types
			private String getFileType(Path path) {
				if (!path.toString().endsWith(".png")) {
					throw new UnsupportedOperationException("Only png files supported");
				}
				return "image/png";
			}

			private String getPropertyName(String name) {
				String text = name.substring(3);
				return text.substring(0, 1).toLowerCase() + text.substring(1);
			}

		}

	}

	public void setPoolSize(Integer val) {
		poolSize = val;
	}

}
