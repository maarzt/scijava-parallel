
package cz.it4i.parallel;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Map;

import net.imagej.Dataset;

import org.scijava.command.Command;

public interface ParallelWorker extends Closeable{

	public Dataset importData(Path filePath);

	public void exportData(Dataset dataset, Path filePath);

	public void deleteData(Dataset ds);

	public   Map<String, Object> executeCommand(
		Class<? extends Command> commandType, Map<String, ?> map);
	
	
	@Override
	default void close() {
		// do nothing
	}
}
