
package cz.it4i.parallel;

import io.scif.services.DatasetIOService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

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
	public Dataset importData(final Path filePath) {
		try {
			return datasetIOService.open(filePath.toString());
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void exportData(final Dataset dataset, final Path filePath) {
		try {
			datasetIOService.save(dataset, filePath.toString());
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deleteData(final Dataset ds) {
		ds.decrementReferences();
	}

	@Override
	public Map<String, Object> executeCommand(
		final Class<? extends Command> commandType, final Map<String, ?> map)
	{

		// Create a new Object-typed input map
		final Map<String, Object> inputMap = new HashMap<>();
		inputMap.putAll(map);

		// Execute command and return outputs
		try {
			return commandService.run(commandType, true, inputMap).get().getOutputs();
		}
		catch (InterruptedException | ExecutionException e) {
			logService.error(e.getMessage(), e);
		}

		return null;
	}
}
