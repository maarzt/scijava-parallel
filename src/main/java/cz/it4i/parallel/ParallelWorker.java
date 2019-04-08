
package cz.it4i.parallel;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.scijava.command.Command;

public interface ParallelWorker extends Closeable {

	public Object importData(Path filePath);

	public void exportData(Object data, Path filePath);

	public void deleteData(Object ds);

	default Map<String, Object> executeCommand(
		Class<? extends Command> commandType, Map<String, ?> map)
	{
		return executeCommand(commandType.getName(), map);
	}

	public Map<String, Object> executeCommand(String commandTypeName,
		Map<String, ?> map);

	default List<Map<String, Object>> executeCommand(
		final String commandTypeName,
		final List<Map<String, Object>> inputs)
	{
		return inputs.stream().map(input -> executeCommand(commandTypeName, input))
			.collect(Collectors.toList());
	}

	@Override
	default void close() {
		// do nothing
	}
}
