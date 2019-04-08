
package cz.it4i.parallel;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.scijava.command.CommandService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginService;
import org.scijava.thread.ThreadService;

public abstract class SimpleOstravaParadigm implements ParallelizationParadigm {

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
	public List<CompletableFuture<Map<String, Object>>> runAllAsync(
		String command, List<Map<String, Object>> listOfparameters)
	{

		List<List<Map<String, Object>>> chunkedParameters = chunkParameters(
			listOfparameters);

		return chunkedParameters.parallelStream().map(
			inputs -> new AsynchronousExecution(
				command, inputs)).map(ae -> repackCompletable(ae.result, ae.size))
			.flatMap(
					List::stream).collect(Collectors.toList());


	}





	protected List<List<Map<String, Object>>> chunkParameters(
		List<Map<String, Object>> listOfparameters) {
		return Lists.partition(listOfparameters, 24);
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

	private static <T> List<T> process(UnaryOperator<T> func, List<T> input) {
		return input.stream().map(func).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static <T> List<CompletableFuture<T>> repackCompletable(
		CompletableFuture<List<T>> input, int size)
	{
		CompletableFuture<Object[]> array = input.thenApply(list -> list.toArray());
		List<CompletableFuture<T>> result = new LinkedList<>();
		for(int i = 0; i < size; i++) {
			final int index = i;
			result.add(array.thenApply(list -> (T) list[index]));
		}
		return result;
	}
	
	private class AsynchronousExecution {
		
		public final int size;

		public final CompletableFuture<List<Map<String, Object>>> result;

		public AsynchronousExecution(String command,
			List<Map<String, Object>> inputs)
		{
			result = supplyAsync(() -> executeForInputs(command, inputs));
			size = inputs.size();
		}

		private List<Map<String, Object>> executeForInputs(String command,
			List<Map<String, Object>> inputs)
		{
			ParallelWorker pw;
			try {
				pw = workerPool.takeFreeWorker();
			}
			catch (InterruptedException exc) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(exc);
			}
			try (ParameterProcessor parameterProcessor = constructParameterProcessor(
				pw, command))
			{

				return process(parameterProcessor::processOutput, pw.executeCommand(
					command, process(parameterProcessor::processInputs, inputs)));
			}
			finally {
				workerPool.addWorker(pw);
			}
		}
	}
}
