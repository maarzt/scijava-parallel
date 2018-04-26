package cz.it4i.parallel;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;

public class LocalMultithreadedPluginWorker implements ParallelWorker {

	@Parameter
	private CommandService commandService;

	@Parameter
	private DatasetIOService datasetIOService;

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

		// Retrieve command and replace GUIDs in inputs where applicable
		CommandInfo commandInfo = commandService.getCommand(commandType);
		if (commandInfo != null) {
			// Execute command and cache outputs
			Map<String, Object> outputs = null;
			try {
				outputs = commandService.run(commandInfo, true, inputMap).get().getOutputs();
				return outputs;
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public Map<String, String> getCommandArgumentsMap(String commandName) {
		// TODO Auto-generated method stub
		return null;
	}
}
