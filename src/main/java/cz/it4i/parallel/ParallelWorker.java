package cz.it4i.parallel;
import java.util.Map;

import org.scijava.command.Command;

public interface ParallelWorker {
	
	public String uploadFile(String filePath, String contentType, String name);
	public void downloadFile(String id, String filePath, String contentType);

	public String deleteResource(String id);
	
	public <T extends Command> String executeCommand(Class<T> commandType, Map<String, ?> map);
	public String getResult();
	
	public Map<String, String> getCommandArgumentsMap(String commandName);
	
}
