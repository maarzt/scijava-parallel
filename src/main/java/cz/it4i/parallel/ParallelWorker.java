package cz.it4i.parallel;
import java.util.HashMap;
import java.util.Map;

public interface ParallelWorker {

	public String getHostName();
	public int getPort();
	
	public String uploadFile(String filePath, String contentType, String name);
	public void downloadFile(String id, String filePath, String contentType);

	public String deleteResource(String id);
	
	public String executeModule(String moduleId, Map<String, ?> map);
	public String getResult();
	
	public String getCommandByName(String name);
	public HashMap<String, String> getCommandArgumentsMap(String commandName);
	
}
