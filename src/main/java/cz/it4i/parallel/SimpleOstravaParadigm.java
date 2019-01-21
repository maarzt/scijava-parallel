
package cz.it4i.parallel;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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
	public List<Map<String, ?>> runAll(List<Class<? extends Command>> commands,
		List<Map<String, ?>> parameters)
	{
		
		Iterator<Map<String, ?>> parIterator = parameters.iterator();
		final Collection<Future<Map<String,Object>>> futures = Collections.synchronizedCollection(
			new LinkedList<>());
		
		for(Class<? extends Command> clazz: commands) {
			futures.add(threadService.run(new Callable<Map<String,Object>>() {
				
				@Override
				public Map<String,Object> call() {
					try {
						ParallelWorker pw = workerPool.takeFreeWorker();
						try {
							return pw.executeCommand(clazz, parIterator.next());
						} finally {
							workerPool.addWorker(pw);
						}
					}
					catch (InterruptedException exc) {
						log.error(exc.getMessage(), exc);
						throw new RuntimeException(exc);
					}
				}
			}));
		}
		return futures.stream().map(f -> {
			try {
				return f.get();
			}
			catch (InterruptedException | ExecutionException exc) {
				log.error("f.get", exc);
				throw new RuntimeException(exc);
			}
		}).collect(Collectors.toList());
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
