package cz.it4i.parallel;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.json.JSONObject;
import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.ParallelTask;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = ParallelizationParadigm.class)
public class SimpleOstravaParadigm extends AbstractParallelizationParadigm {

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.SimpleOstravaParadigm.class);
	
	private WorkerPool workerPool;
	
	@Override
	public void init() {
		
		retrieveConnectionConfig();
		if (connectionConfig.size() == 0) {
			Map<String, String> configEntries = new LinkedHashMap<>();
			configEntries.put("address", "localhost");
			configEntries.put("port", "8080");
			updateConnectionConfig(configEntries);
		}
		workerPool = new WorkerPool();
	}

	@Override
	public void submit() {
		// TODO Auto-generated method stub, consider moving to
		// AbstractParallelizationParadigm

	}

	@Override
	public <T> void parallelLoop(Iterable<T> arguments, BiConsumer<T, ParallelTask> consumer) {
		arguments.forEach(val->
		{
			try(P_ParallelTask task = new P_ParallelTask()){
				consumer.accept(val,null);
			}
		});

	}
	
	
	private class P_ParallelTask implements ParallelTask, Closeable {

		private ParallelWorker p;
		
		public P_ParallelTask() {
			try {
				p = workerPool.takeFreeWorker();
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getRemoteModule(Class<T> type) {
			return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] {type}, new P_InvocationHandler(type, p));
		}

		@Override
		public void close() {
			workerPool.addWorker(p);
		}
		
	}

	private class P_InvocationHandler implements InvocationHandler {

		private Map<String,Object> args = new HashMap<>();
		private String typeName;
		private ParallelWorker worker;
		private String executeResult;
		
		
		public P_InvocationHandler(Class<?> type, ParallelWorker worker) {
			this.typeName = getTypeName(type);
			this.worker = worker;
		}
		
		//TODO make better connection to real type - e.g. based on annotations 
		private String getTypeName(Class<?> type) {
			return type.getPackage().getName() + type.getSimpleName().substring(1);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if(method.getName().startsWith("set")) {
				setValue(getPropertyName(method.getName()), args[0]);
			} else if(method.getName().equals("run")) {
				// execute worker
				executeResult = worker.executeModule( "command:"+typeName,  this.args);

			} else if(method.getName().startsWith("get")) {
				return getValue(getPropertyName(method.getName()),method.getReturnType());
			}
			return null;
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

		//TODO: make conversion more flexible 
		private void setValue(String propertyName, Object object) {
			if (object instanceof Path) {
				Path path = (Path) object;
				String ret = worker.uploadFile(path.toAbsolutePath().toString(), getFileType(path), path.getFileName().toString());
				// obtain uploaded file id
				object = new org.json.JSONObject(ret).getString("id");

			}
			args.put(propertyName, object);
		}

		//TODO: support another types
		private String getFileType(Path path) {
			if(!path.endsWith(".png")) {
				throw new UnsupportedOperationException("Only png files supported");
			}
			return "image/png";
		}

		private String getPropertyName(String name) {
			String text = name.substring(3);
			return text.substring(0,1).toLowerCase() + text.substring(1);
		}
		
	}

}
