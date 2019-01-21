
package cz.it4i.parallel;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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
	public RemoteDataset createRemoteDataset(URI uri) {
		return new P_RemoteDataset(uri);
	}
	
	@Override
	public void close() {
		super.close();
		workerPool.close();
		threadService.dispose();
	}
	
	private class P_RemoteDataset extends RemoteDataset {

		public P_RemoteDataset(URI uri) {
			super(uri);
		}
		
		
		
	}
		
		
		
	

}
