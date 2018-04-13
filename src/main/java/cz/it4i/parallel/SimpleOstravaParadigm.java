package cz.it4i.parallel;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.nio.file.Path;
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

	private ForkJoinPool forkJoinPool;

	@Override
	public void init() {
		workerPool = new WorkerPool();
		initWorkerPool();
		if (forkJoinPool != null) {
			forkJoinPool.shutdown();
		}
		forkJoinPool = new ForkJoinPool(poolSize);

	}

	@Override
	public <T> void parallelLoop(Iterable<T> arguments, BiConsumer<T, ParallelTask> consumer) {
		forkJoinPool.submit(() -> StreamSupport.stream(arguments.spliterator(), true).forEach(val -> {
			try (P_ParallelTask task = new P_ParallelTask()) {
				consumer.accept(val, task);
			}
		})).join();
	}

	public void setPoolSize(Integer val) {
		poolSize = val;
	}

	abstract protected void initWorkerPool();

	private class P_ParallelTask implements ParallelTask, Closeable {

		private ParallelWorker worker;

		public P_ParallelTask() {
			try {
				worker = workerPool.takeFreeWorker();
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}

		@Override
		public <T> T getRemoteModule(Class<T> type) {
			return (T) Mockito.mock(type, new P_InvocationHandler<T>(type));
		}

		@Override
		public void close() {
			workerPool.addWorker(worker);
		}

		@Override
		public Dataset importData(Path path) {
			Dataset result = worker.importData(path);
			return result;
		}

		@Override
		public void exportData(Dataset ds, Path p) {
			worker.exportData(ds, p);
			worker.deleteData(ds);
		}

		private class P_InvocationHandler<T> implements Answer<T> {

			private final Map<String, Object> args = new HashMap<>();
			private final Class<?> type;
			private Map<String, Object> executeResult;

			public P_InvocationHandler(Class<?> type) {
				this.type = resolveType(type);
			}

			@SuppressWarnings("unchecked")
			@Override
			public T answer(InvocationOnMock invocation) throws Throwable {

				Method method = invocation.getMethod();
				Object[] args = invocation.getArguments();
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
				return executeResult.get(propertyName);
			}

			private void setValue(String propertyName, Object object) {
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
}
