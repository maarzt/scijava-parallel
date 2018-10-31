
package org.scijava.parallel;

import java.io.Closeable;
import java.nio.file.Path;

import net.imagej.Dataset;

import org.scijava.command.Command;

public interface ExecutionContext extends Closeable{

	<T extends Command> T getRemoteCommand(Class<T> type);

	Dataset importData(Path dataset);

	void exportData(Dataset ds, Path p);
	
	@Override
	void close();
}
