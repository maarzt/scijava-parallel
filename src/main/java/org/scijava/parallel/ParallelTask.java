package org.scijava.parallel;

public interface ParallelTask {

	<T> T getRemoteModule(Class<T> type);
}
