package cz.it4i.parallel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Parameter;

import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.plugins.commands.imglib.RotateImageXY;

public class LocalPluginWorker implements ParallelWorker {

	@Parameter
	private CommandService commandService;
	
	@Parameter
	private DatasetIOService datasetIOService;
	
	@Parameter
	private Context context;
	
	public LocalPluginWorker() {
		new Context().inject(this);
	}	
	
	private final Map<String, String> cachedFilePaths = new HashMap<>();

	@Override
	public String uploadFile(String filePath, String contentType, String name) {
		String filePathIdentifier = UUID.randomUUID().toString();
		cachedFilePaths.put(filePathIdentifier, filePath);
		return filePathIdentifier;
	}

	@Override
	public void downloadFile(String id, String filePath, String contentType) {
		// TODO Auto-generated method stub

	}

	@Override
	public String deleteResource(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Command> String executeCommand(Class<T> commandType, Map<String, ?> map) {
		
		// Create a new Object-typed input map
		Map<String, Object> inputMap = new HashMap<>();
		inputMap.putAll(map);
		
		// TODO: Remove this hack		
		// Retrieve command and replace GUIDs in inputs where applicable
		CommandInfo commandInfo = commandService.getCommand(RotateImageXY.class);
		if (commandInfo != null) {			
			for (final ModuleItem<?> input : commandInfo.inputs()) {
				if (Dataset.class.isAssignableFrom(input.getType())) {
					final Object datasetIdentifier = inputMap.get(input.getName());
					if (datasetIdentifier instanceof String) {
						final String filepath = cachedFilePaths.get(datasetIdentifier);
						try {
							inputMap.replace(input.getName(), datasetIOService.open(filepath));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			
			Map<String, Object> outputs = null;
			try {
				outputs = commandService.run(commandInfo, true, inputMap).get().getOutputs();	
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return null;
	}

	@Override
	public String getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<String, String> getCommandArgumentsMap(String commandName) {
		// TODO Auto-generated method stub
		return null;
	}

}
