package cz.it4i.parallel;

import java.util.HashMap;
import java.util.Map;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;

public class LocalPluginWorker implements ParallelWorker {

	@Parameter
	private CommandService service;
	
	public LocalPluginWorker() {
		new Context().inject(this);
	}

	@Override
	public String uploadFile(String filePath, String contentType, String name) {
		// TODO Auto-generated method stub
		return null;
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
		// commandService.getCommand(moduleId);
		return null;
	}

	@Override
	public String getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCommandByName(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<String, String> getCommandArgumentsMap(String commandName) {
		// TODO Auto-generated method stub
		return null;
	}

}
