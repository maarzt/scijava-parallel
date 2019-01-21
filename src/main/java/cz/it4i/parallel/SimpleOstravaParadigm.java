
package cz.it4i.parallel;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import net.imagej.Dataset;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.RemoteDataset;
import org.scijava.parallel.WritableDataset;
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
							Map<String,?> params = parIterator.next();
							params = processInputDataset(pw, params);
							return processOutputDataset(pw, pw.executeCommand(clazz, params));
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
	public RemoteDataset createRemoteDataset(URI uri) {
		return new P_RemoteDataset(uri);
	}
	
	@Override
	public void exportWritableDatased(WritableDataset writableDataset, URI uri) {
		P_WritableDataset wd = (P_WritableDataset) writableDataset;
		wd.export(uri);
	}
	
	@Override
	public void close() {
		super.close();
		workerPool.close();
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
		
	private class P_WritableDataset extends WritableDataset {
		
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
