package cz.it4i.parallel;

import java.nio.file.Path;
import java.util.Map;

import org.scijava.command.Command;

import net.imagej.Dataset;

public interface ParallelWorker {

	public Dataset importData(Path filePath);

	public void exportData(Dataset dataset, Path filePath);

	public void deleteData(Dataset ds);

	public <T extends Command> Map<String, Object> executeCommand(Class<T> commandType, Map<String, ?> map);

	public Map<String, String> getCommandArgumentsMap(String commandName);
}
