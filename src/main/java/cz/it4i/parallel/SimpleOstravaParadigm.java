
package cz.it4i.parallel;

import com.google.common.collect.Streams;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import net.imagej.Dataset;

import org.scijava.command.CommandService;
import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.RemoteDataset;
import org.scijava.parallel.WriteableDataset;
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
	
	private ExecutorService executorService;

	// -- SimpleOstravaParadigm methods --

	abstract protected void initWorkerPool();

	// -- ParallelizationParadigm methods --

	@Override
	public void init() {
		workerPool = new WorkerPool();
		initWorkerPool();
		executorService = Executors.newFixedThreadPool(workerPool.size(), threadService);
	}
	
	@Override
	public List<Map<String, ?>> runAllCommands(List<String> commands,
		List<Map<String, ?>> parameters)
	{
		List<CompletableFuture<Map<String, ?>>> futures = runAllCommandsAsync(commands,
			parameters);
			
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
	public List<CompletableFuture<Map<String, ?>>> runAllCommandsAsync(
		List<String> commands, List<Map<String, ?>> parameters)
	{
		List<CompletableFuture<Map<String, ?>>> futures = Streams.zip(commands.stream(), parameters.stream(), new BiFunction<String, Map<String, ?>, CompletableFuture<Map<String, ?>>>() {

			@Override
			public CompletableFuture<Map<String, ?>> apply(String commmand,
				Map<String, ?> params)
			{
				return CompletableFuture.supplyAsync(new Supplier<Map<String, ?>>() {

					@Override
					public Map<String, ?> get() {
						Map<String, ?> localParams = params;
						try {
							ParallelWorker pw = workerPool.takeFreeWorker();
							try {
								localParams = processInputDataset(pw, localParams);
								return processOutputDataset(pw, pw.executeCommand(commmand, localParams));
							} finally {
								workerPool.addWorker(pw);
							}
						}
						catch (InterruptedException exc) {
							log.error(exc.getMessage(), exc);
							throw new RuntimeException(exc);
						}
					}
				}, executorService);
			}
		}).collect(Collectors.toList());
		return futures;
	}
	
	@Override
	public RemoteDataset createRemoteDataset(URI uri) {
		return new P_RemoteDataset(uri);
	}
	
	@Override
	public void exportWriteableDataset(WriteableDataset writableDataset, URI uri) {
		P_WritableDataset wd = (P_WritableDataset) writableDataset;
		wd.export(uri);
	}
	
	@Override
	public void close() {
		super.close();
		workerPool.close();
		executorService.shutdown();
		threadService.dispose();
	}
	
	private Map<String, ?> processInputDataset(ParallelWorker pw,
		Map<String, ?> params)
	{
		return params.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> {
			Object val = e.getValue();
			if (val instanceof RemoteDataset) {
				P_RemoteDataset remoteDataset = (P_RemoteDataset) val;
				return remoteDataset.importIfNeaded(pw);
			} else if (val instanceof WriteableDataset) {
				P_WritableDataset wd = (P_WritableDataset) val;
				if (wd.pw != pw) {
					throw new AssertionError("writable dataset is loaded in " + wd.pw + " but used " + pw);
				}
			}
			return val;
		}));
	}
	
	private Map<String, Object> processOutputDataset(ParallelWorker pw,
		Map<String, Object> executeCommand)
	{
		return executeCommand.entrySet().stream().collect(Collectors.toMap(e->e.getKey(), e-> {
			Object val = e.getValue();
			if (val instanceof Dataset) {
				Dataset ds = (Dataset) val;
				return new P_WritableDataset(pw, ds);
			}
			return val;
		}));
	}

	private class P_RemoteDataset extends RemoteDataset {

		private Map<ParallelWorker, Dataset> usedDataset = new HashMap<>();
		
		
		public P_RemoteDataset(URI uri) {
			super(uri);
		}
		
		synchronized public Dataset importIfNeaded(ParallelWorker pw) {
			return usedDataset.computeIfAbsent(pw, _pw -> pw.importData(Paths.get(getUri())));
		}
		
	}
		
	private class P_WritableDataset extends WriteableDataset {
		
		private ParallelWorker pw;
		private Dataset dataset;
		public P_WritableDataset(ParallelWorker pw, Dataset dataset) {
			super();
			this.pw = pw;
			this.dataset = dataset;
		}
		
		public void export(URI uri) {
			pw.exportData(dataset, Paths.get(uri));
		}
	}
		
	

}
