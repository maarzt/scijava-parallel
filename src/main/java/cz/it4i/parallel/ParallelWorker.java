package cz.it4i.parallel;
import java.util.Map;

import org.scijava.command.Command;

public interface ParallelWorker {
	
	public String uploadFile(String filePath, String name);
	public void downloadFile(String id, String filePath);

	public String deleteResource(String id);
	
	public <T extends Command> Map<String, Object> executeCommand(Class<T> commandType, Map<String, ?> map);
	
	public Map<String, String> getCommandArgumentsMap(String commandName);
	
}
