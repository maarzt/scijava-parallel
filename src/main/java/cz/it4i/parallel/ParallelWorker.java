
package cz.it4i.parallel;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Map;

import org.scijava.command.Command;

public interface ParallelWorker extends Closeable {

	public Object importData(Path filePath);

	public void exportData(Object data, Path filePath);

	public void deleteData(Object ds);

	default public Map<String, Object> executeCommand(
		Class<? extends Command> commandType, Map<String, ?> map)
	{
		return executeCommand(commandType.getName(), map);
	}

	public Map<String, Object> executeCommand(String commandTypeName,
		Map<String, ?> map);

	@Override
	default void close() {
		// do nothing
	}
}
