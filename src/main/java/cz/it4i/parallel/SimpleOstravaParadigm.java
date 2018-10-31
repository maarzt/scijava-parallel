
package cz.it4i.parallel;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import net.imagej.Dataset;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.ExecutionContext;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleOstravaParadigm extends
	AbstractParallelizationParadigm
{

	private static final Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.SimpleOstravaParadigm.class);

	protected WorkerPool workerPool;

	@Parameter
	private ThreadService threadService;

	@Parameter
	private CommandService commandService;

	// -- SimpleOstravaParadigm methods --

	abstract protected void initWorkerPool();

	// -- ParallelizationParadigm methods --

	@Override
	public void init() {
		workerPool = new WorkerPool();
		initWorkerPool();
	}

	@Override
	public <T> void parallelFor(final Iterable<T> arguments,
		final BiConsumer<T, ExecutionContext> consumer)
	{
		final Collection<Future<?>> futures = Collections.synchronizedCollection(
			new LinkedList<>());
		arguments.forEach(val -> futures.add(threadService.run(new Runnable() {

			@Override
			public void run() {
				try (ExecutionContext task = createExecutionContext()) {
					try {
						consumer.accept(val, task);
					}
					catch (final Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		})));
		futures.forEach(f -> {
			try {
				f.get();
			}
			catch (InterruptedException | ExecutionException e) {
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				log.error(e.getMessage(), e);
			}
		});
	}

	@Override
	public ExecutionContext createExecutionContext() {
		return new P_ExecutionContext();
	}
	
	@Override
	public void close() {
		super.close();
		workerPool.close();
		threadService.dispose();
	}
	
	// -- Private classes and helper methods --

	private class P_ExecutionContext implements ExecutionContext {

		private ParallelWorker worker;

		public P_ExecutionContext() {
			try {
				worker = workerPool.takeFreeWorker();
			}
			catch (final InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}

		@Override
		public <T extends Command> T getRemoteCommand(final Class<T> type) {

			// Create a new command so that its input parameters are properly
			// initialized
			final T realCommand = commandService.create(type);

			// Create a mocked command which will call real command methods...
			final T mockedCommand = mock(type,
				(Answer<Object>) invocation -> invocation.getMethod().invoke(
					realCommand, invocation.getArguments()));

			// ...apart from run() which will be customized
			doAnswer(new Answer<Void>() {

				@Override
				public Void answer(final InvocationOnMock invocation) throws Throwable {
					final CommandInfo cInfo = commandService.getCommand(type);
					final CommandModule cModule = new CommandModule(cInfo, realCommand);
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
		public Dataset importData(final Path path) {
			return worker.importData(path);
		}

		@Override
		public void exportData(final Dataset ds, final Path p) {
			worker.exportData(ds, p);
			worker.deleteData(ds);
		}
	}
}
