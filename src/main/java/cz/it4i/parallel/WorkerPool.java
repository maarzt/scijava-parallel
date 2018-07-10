
package cz.it4i.parallel;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WorkerPool {

	private final BlockingQueue<ParallelWorker> availableWorkers;

	public WorkerPool() {
		availableWorkers = new ArrayBlockingQueue<>(1024);
	}

	public void addWorker(final ParallelWorker worker) {
		availableWorkers.add(worker);
	}

	public ParallelWorker takeFreeWorker() throws InterruptedException {
		return availableWorkers.take();
	}
}
