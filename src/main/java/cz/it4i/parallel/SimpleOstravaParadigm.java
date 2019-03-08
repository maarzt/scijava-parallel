
package cz.it4i.parallel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.scijava.command.CommandService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginService;
import org.scijava.thread.ThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleOstravaParadigm implements ParallelizationParadigm {

	private static final Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.SimpleOstravaParadigm.class);

	protected WorkerPool workerPool;

	@Parameter
	private ThreadService threadService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private PluginService pluginService;

	private Map<Class<?>, ParallelizationParadigmConverter<?>> mappers;

	private ExecutorService executorService;

	// -- SimpleOstravaParadigm methods --

	abstract protected void initWorkerPool();

	// -- ParallelizationParadigm methods --

	@Override
	public void init() {
		workerPool = new WorkerPool();
		initWorkerPool();
		executorService = Executors.newFixedThreadPool(workerPool.size(),
			threadService);
	}

	@Override
	public List<Map<String, Object>> runAll(String commandName,
		List<Map<String, Object>> parameters)
	{
		List<CompletableFuture<Map<String, Object>>> futures = runAllAsync(
			commandName, parameters);

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
	public List<CompletableFuture<Map<String, Object>>> runAllAsync(
		String command, List<Map<String, Object>> listOfparameters)
	{
		return listOfparameters.parallelStream().map(parameters -> CompletableFuture
			.supplyAsync(() -> {
				try {
					ParallelWorker pw = workerPool.takeFreeWorker();
					try (ParameterProcessor parameterProcessor =
						constructParameterProcessor(pw, command))
			{

						return parameterProcessor.processOutput(pw.executeCommand(command,
							parameterProcessor.processInputs(parameters)));
					}
					finally {
						workerPool.addWorker(pw);
					}
				}
				catch (InterruptedException exc) {
					log.error(exc.getMessage(), exc);
					throw new RuntimeException(exc);
				}
			}, executorService)).collect(Collectors.toList());
	}

	@Override
	public void close() {
		workerPool.close();
		executorService.shutdown();
		threadService.dispose();
	}

	protected abstract ParameterTypeProvider getTypeProvider();

	protected ParameterProcessor constructParameterProcessor(ParallelWorker pw,
		String command)
	{
		return new DefaultParameterProcessor(getTypeProvider(), command, pw,
			getMappers());
	}

	synchronized private Map<Class<?>, ParallelizationParadigmConverter<?>>
		getMappers()
	{
		if (mappers == null) {
			mappers = new HashMap<>();
			initMappers();
		}
		return mappers;
	}

	private void initMappers() {
		pluginService.createInstancesOfType(
			ParallelizationParadigmConverter.class).stream().filter(
				m -> isParadigmSupportedBy(m)).forEach(m -> mappers.put(m
					.getOutputType(), m));

	}

	private boolean isParadigmSupportedBy(
		ParallelizationParadigmConverter<?> m)
	{
		for (Class<? extends ParallelizationParadigm> clazz : m
			.getSupportedParadigms())
		{
			if (clazz.isAssignableFrom(this.getClass())) {
				return true;
			}
		}
		return false;
	}

}
