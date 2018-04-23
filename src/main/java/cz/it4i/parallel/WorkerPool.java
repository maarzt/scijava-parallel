package cz.it4i.parallel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WorkerPool
{
	BlockingQueue<ParallelWorker> availableWorkers;
	
	
	public WorkerPool() {
		availableWorkers = new ArrayBlockingQueue<ParallelWorker>(1024);
	}
	
	public void addWorker(ParallelWorker worker) {
		availableWorkers.add(worker);
	}
	
	public ParallelWorker takeFreeWorker() throws InterruptedException {
		return availableWorkers.take();	
	}
	
}
