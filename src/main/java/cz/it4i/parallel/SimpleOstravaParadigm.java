
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
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import net.imagej.Dataset;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.RemoteDataset;
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
	
	private Map<ParallelWorker,Map<RemoteDataset, Dataset>> usedDataset = new HashMap<>();

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
							params = processDataset(pw, params);
							return pw.executeCommand(clazz, params);
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
	public void close() {
		super.close();
		workerPool.close();
		threadService.dispose();
	}
	
	private Map<String, ?> processDataset(ParallelWorker pw,
		Map<String, ?> params)
	{
		Map<String, Object> result = new HashMap<>();
		for(Entry<String,?> entry: params.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof RemoteDataset) {
				RemoteDataset rd = (RemoteDataset) entry.getValue();
				Map<RemoteDataset,Dataset> map = usedDataset.computeIfAbsent(pw, __ -> new HashMap<>());
				Dataset ds = map.computeIfAbsent(rd, rdd -> pw.importData(Paths.get(rdd.getUri())));
				value = ds;
			} 
			result.put(entry.getKey(), value);
		}
		return result;
	}
	
	private class P_RemoteDataset extends RemoteDataset {

		public P_RemoteDataset(URI uri) {
			super(uri);
		}
		
		
		
	}
		
		
		
	

}
