package cz.it4i.parallel;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.ParallelTask;
import org.scijava.plugin.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.imagej.Dataset;

public abstract class SimpleOstravaParadigm extends AbstractParallelizationParadigm {

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.SimpleOstravaParadigm.class);

	protected Integer poolSize;

	protected WorkerPool workerPool;

	private ForkJoinPool forkJoinPool;

	@Parameter
	private CommandService commandService;

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
		public <T extends Command> T getRemoteCommand(Class<T> type) {

			// Create a new command so that its input parameters are properly initialized
			T realCommand = commandService.create(type);
			
			// Create a mocked command which will call real command methods...
			T mockedCommand = mock(type, (Answer<Object>) invocation -> invocation.getMethod().invoke(realCommand,
					invocation.getArguments()));
			
			// ...apart from run() which will be customized
			doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) throws Throwable {
					CommandInfo cInfo = commandService.getCommand(type);
					CommandModule cModule = new CommandModule(cInfo, realCommand);
					cModule.setOutputs(worker.executeCommand(type, cModule.getInputs()));
					return null;
				}
			}).when(mockedCommand).run();
			
			return mockedCommand;
		}

		@Override
		public void close() {
			workerPool.addWorker(worker);
		}

		@Override
		public Dataset importData(Path path) {
			return worker.importData(path);
		}

		@Override
		public void exportData(Dataset ds, Path p) {
			worker.exportData(ds, p);
			worker.deleteData(ds);
		}
	}
}
