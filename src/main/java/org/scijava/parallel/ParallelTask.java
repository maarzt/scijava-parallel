package org.scijava.parallel;

import java.nio.file.Path;

import net.imagej.Dataset;

public interface ParallelTask {

	<T> T getRemoteModule(Class<T> type);
	
	Dataset importData(Path dataset);
	
	void exportData(Dataset ds, Path p);

}
