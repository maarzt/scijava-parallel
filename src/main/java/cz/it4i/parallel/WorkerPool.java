package cz.it4i.parallel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WorkerPool
{
	BlockingQueue<ParallelWorker> availableWorkers;
	BlockingQueue<ParallelWorker> finishedWorkers;
	List<ParallelWorker> processingWorkers;
	
	
	public WorkerPool() {
		availableWorkers = new ArrayBlockingQueue<ParallelWorker>(1024);
		finishedWorkers = new ArrayBlockingQueue<ParallelWorker>(1024);
		processingWorkers = new ArrayList<ParallelWorker>();
	}
	
	public void addWorker(ParallelWorker worker) {
		availableWorkers.add(worker);
	}
	
	public ParallelWorker takeFreeWorker() throws InterruptedException {
		return availableWorkers.take();	
	}
	
	public ParallelWorker getFinishedWorker() throws InterruptedException {
		return finishedWorkers.take();
	}
	
	public boolean hasAvailableWorkers() {
		return (!availableWorkers.isEmpty());
	}
	
	public boolean hasFinishedWorkers() {
		return (!finishedWorkers.isEmpty());
	}
	
	public boolean hasProcessingWorkers() {
		return (!processingWorkers.isEmpty());
	}
	
	/*public void execute(ParallelWorker worker, String moduleId, Map<String, ?> map) {
		processingWorkers.add(worker);
		new Thread() {
			public void run() {
				worker.executeModule(moduleId, map);
				processingWorkers.remove(worker);
				
				finishedWorkers.add(worker);
			}
		}.start();
	
	}*/
	
}
