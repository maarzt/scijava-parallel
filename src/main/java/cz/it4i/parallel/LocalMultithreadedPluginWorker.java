package cz.it4i.parallel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;

public class LocalMultithreadedPluginWorker implements ParallelWorker {

	@Parameter
	private CommandService commandService;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private LogService logService;

	@Parameter
	private Context context;

	public LocalMultithreadedPluginWorker() {
		new Context().inject(this);
	}

	@Override
	public Dataset importData(Path filePath) {
		try {
			return datasetIOService.open(filePath.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void exportData(Dataset dataset, Path filePath) {
		try {
			datasetIOService.save(dataset, filePath.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deleteData(Dataset ds) {
		ds.decrementReferences();
	}

	@Override
	public <T extends Command> Map<String, Object> executeCommand(Class<T> commandType, Map<String, ?> map) {

		// Create a new Object-typed input map
		Map<String, Object> inputMap = new HashMap<>();
		inputMap.putAll(map);

		// Execute command and return outputs
		try {
			return commandService.run(commandType, true, inputMap).get().getOutputs();
		} catch (InterruptedException | ExecutionException e) {
			logService.error(e.getMessage(), e);
		}

		return null;
	}
}
