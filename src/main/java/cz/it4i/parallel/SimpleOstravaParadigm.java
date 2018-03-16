package cz.it4i.parallel;

import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

import org.scijava.command.Command;
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

		@SuppressWarnings("unchecked")
		@Override
		public <T> void run(Class<T> type, Map<String, Object> inputMap) {
			if (Command.class.isAssignableFrom(type)) {
				executeResult = worker.executeCommand((Class<Command>) type, inputMap);
			}
		}

		private class P_InvocationHandler implements InvocationHandler {

			private final Map<String, Object> args = new HashMap<>();
			private final Class<?> type;
			
			public P_InvocationHandler(Class<?> type) {
				this.type = type;
			}

			

			@SuppressWarnings("unchecked")
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName().startsWith("set")) {
					setValue(getPropertyName(method.getName()), args[0]);
				} else if (method.getName().equals("run")) {
					// execute worker
					if (Command.class.isAssignableFrom(type)) {
						executeResult = worker.executeCommand((Class<Command>) this.type, this.args);
					}					
				} else if (method.getName().startsWith("get")) {
					return getValue(getPropertyName(method.getName()), method.getReturnType());
				}
				return null;
			}
			
			private Object getValue(String propertyName, Class<?> returnType) {
				return SimpleOstravaParadigm.this.getValue(worker, args, executeResult, propertyName, returnType);
			}

			// TODO: make conversion more flexible
			private void setValue(String propertyName, Object object) {
				SimpleOstravaParadigm.this.setValue(worker, args, executeResult, propertyName, object);
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

	
	abstract protected void setValue(ParallelWorker worker, Map<String, Object> args, String executeResult, String propertyName,
			Object object);

	abstract protected Object getValue(ParallelWorker worker, Map<String, Object> args, String executeResult,
			String propertyName, Class<?> returnType); 

}
