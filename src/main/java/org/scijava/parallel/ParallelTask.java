package org.scijava.parallel;

import java.nio.file.Path;

import org.scijava.command.Command;

import net.imagej.Dataset;

public interface ParallelTask {

	<T extends Command> T getRemoteCommand(Class<T> type);
	
	Dataset importData(Path dataset);
	
	void exportData(Dataset ds, Path p);

}
