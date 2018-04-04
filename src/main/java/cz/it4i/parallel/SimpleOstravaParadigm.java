package cz.it4i.parallel;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.scijava.command.Command;
import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.ParallelTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.imagej.Dataset;

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

		private Map<Dataset,String> mockedData2id = new HashMap<>();
		
		private Map<String, Dataset> id2mockedData = new HashMap<>();
		
		private Map<String,String> id2Suffix = new HashMap<>();
		
		
		public P_ParallelTask() {
			try {
				worker = workerPool.takeFreeWorker();
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}

		@Override
		public <T> T getRemoteModule(Class<T> type) {
			return (T) Mockito.mock(type,
					new P_InvocationHandler<T>(type));
		}

		@Override
		public void close() {
			workerPool.addWorker(worker);
		}

		@Override
		public Dataset importData(Path path) {
			String obj = worker.uploadFile(path.toAbsolutePath().toString(),
					path.getFileName().toString());
			Dataset result = Mockito.mock(Dataset.class, p->{throw new UnsupportedOperationException();});
			mockedData2id.put(result, obj);
			id2mockedData.put(obj, result);
			id2Suffix.put(obj, path.toString().substring(path.toString().lastIndexOf('.')));
			return result;
		}

		@Override
		public Path exportData(Dataset ds) {
			String id = mockedData2id.get(ds);
			Path p = Paths.get("/tmp/output/" + id + id2Suffix.get(id));
			worker.downloadFile(id, p.toString());
			worker.deleteResource(id);
			mockedData2id.remove(ds);
			id2Suffix.remove(id);
			return p;
		}

		private class P_InvocationHandler<T> implements Answer<T>{

			private final Map<String, Object> args = new HashMap<>();
			private final Class<?> type;
			private Map<String,Object> executeResult;
			
			public P_InvocationHandler(Class<?> type) {
				this.type = resolveType(type);
			}

			@SuppressWarnings("unchecked")
			@Override
			public T answer(InvocationOnMock invocation) throws Throwable {
				
				Method method = invocation.getMethod();
				Object [] args = invocation.getArguments();
				if (method.getName().startsWith("set")) {
					setValue(getPropertyName(method.getName()), args[0]);
				} else if (method.getName().equals("run")) {
					// execute worker
					if (Command.class.isAssignableFrom(type)) {
						executeResult = worker.executeCommand((Class<Command>) this.type, this.args);
					}					
				} else if (method.getName().startsWith("get")) {
					return (T) getValue(getPropertyName(method.getName()), method.getReturnType());
				}
				return null;
			}
			
			private Object getValue(String propertyName, Class<?> returnType) {
				Object result = executeResult.get(propertyName);
				if (returnType.equals(Dataset.class)) {
					String id = (String) result;
					result = id2mockedData.get(id);
				}
				return result;
			}

			// TODO: make conversion more flexible
			private void setValue(String propertyName, Object object) {
				if (object instanceof Dataset) {
					Dataset ds = (Dataset) object;
					object = mockedData2id.get(ds);
				}
				args.put(propertyName, object);
			}

			private String getPropertyName(String name) {
				String text = name.substring(3);
				return text.substring(0, 1).toLowerCase() + text.substring(1);
			}
			
			private Class<?> resolveType(Class<?> inputType) {				
				return inputType;
			}
		}

	}

	public void setPoolSize(Integer val) {
		poolSize = val;
	}
	
}
