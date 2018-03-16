package cz.it4i.parallel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.scijava.command.Command;

public class ImageJServerWorker implements ParallelWorker {

	private String hostName;
	private int port;
	String result;
	


	ImageJServerWorker(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	
	public String getHostName() {
		return hostName;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public String getResult() {
		return result;
	}

	public String uploadFile(String filePath, String contentType, String name) {

		String json = null;

		HttpEntity entity = MultipartEntityBuilder.create()
				.addBinaryBody("file", new File(filePath), ContentType.create(contentType), name).build();

		HttpClient httpClient = HttpClientBuilder.create().build();

		try {

			HttpPost httpPost = new HttpPost("http://" + hostName + ":" + String.valueOf(port) + "/objects/upload");
			httpPost.setEntity(entity);
			HttpResponse response = httpClient.execute(httpPost);
			json = EntityUtils.toString(response.getEntity());

			// TODO check result code

		} catch (Exception e) {
			e.printStackTrace();
		}

		return json;
	}

	public void downloadFile(String id, String filePath, String contentType) {

		String getUrl = "http://" + hostName + ":" + String.valueOf(port) + "/objects/" + id + "/" + contentType;
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(getUrl);

		try {

			HttpResponse response = httpClient.execute(get);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				// long len = entity.getContentLength();
				BufferedInputStream bis = new BufferedInputStream(entity.getContent());
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
				int inByte;
				while ((inByte = bis.read()) != -1) {
					bos.write(inByte);
				}
				bis.close();
				bos.close();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@SuppressWarnings("unchecked")
	public <T extends Command> String executeCommand(Class<T> commandType, Map<String, ?> map) {
		
		String json = null;

		String postUrl = "http://" + hostName + ":" + String.valueOf(port) + "/modules/" + "command:" + getTypeName(commandType);
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(postUrl);

		try {

			JSONObject reqJson = new JSONObject();

			for(Map.Entry<String,?> pair: map.entrySet()) {
				reqJson.put(pair.getKey(), pair.getValue());
			}

			StringEntity postingString = new StringEntity(reqJson.toString());
			post.setEntity(postingString);
			post.setHeader("Content-type", "application/json");
			HttpResponse response = httpClient.execute(post);

			json = EntityUtils.toString(response.getEntity());

		} catch (Exception e) {
			e.printStackTrace();
		}

		result = json;
		return json;
	}

	public String deleteResource(String id) {
		String json = null;

		String postUrl = "http://" + hostName + ":" + String.valueOf(port) + "/objects/" + id;
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpDelete delete = new HttpDelete(postUrl);

		try {

			HttpResponse response = httpClient.execute(delete);
			json = EntityUtils.toString(response.getEntity());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return json;
	}

	private void addCommandsToMap(JSONObject jsonObj, HashMap<String, String> commandMap) {
		String command = null;
		String label = null;
		for (Object keyObj : jsonObj.keySet()) {
			String key = (String) keyObj;
			Object valObj = jsonObj.get(key);
			if (valObj instanceof JSONObject) {
				// call printJSON on nested object
				if (valObj != null)
					addCommandsToMap((JSONObject) valObj, commandMap);
			} else {
				// store key-value pair
				if (valObj != null) {
					if (key.contains("Command")) {
						command = valObj.toString();
					}
					if (key.contains("Label")) {
						label = valObj.toString();
					}
				}
			}
		}
		if (command != null && label != null) {
			commandMap.put(label, command);
		}
	}

	public String getCommandByName(String name) {

		HashMap<String, String> commandMap = new HashMap<String, String>();

		String getUrl = "http://" + hostName + ":" + String.valueOf(port) + "/admin/menuNew/";
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(getUrl);

		try {

			HttpResponse response = httpClient.execute(get);
			HttpEntity entity = response.getEntity();

			JSONParser jsonParser = new JSONParser();
			JSONObject obj = (JSONObject) jsonParser.parse(new InputStreamReader(entity.getContent(), "UTF-8"));

			addCommandsToMap(obj, commandMap);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return commandMap.get(name);

	}

	public HashMap<String, String> getArgumentsMap(String commandName) {

		HashMap<String, String> argumentMap = new HashMap<String, String>();

		String getUrl = "http://" + hostName + ":" + String.valueOf(port) + "/modules/" + commandName;
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(getUrl);

		try {

			HttpResponse response = httpClient.execute(get);
			HttpEntity entity = response.getEntity();

			JSONParser jsonParser = new JSONParser();
			JSONObject obj = (JSONObject) jsonParser.parse(new InputStreamReader(entity.getContent(), "UTF-8"));

			JSONArray inputs = (JSONArray) obj.get("inputs");
			if (inputs != null) {
				for (int i = 0; i < inputs.size(); i++) {
					argumentMap.put(((JSONObject) inputs.get(i)).get("name").toString(), null);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return argumentMap;

	}
	
	public HashMap<String, String> getCommandArgumentsMap(String commandName) {
		return getArgumentsMap("command:" + getCommandByName(commandName));
	}
	
	private String getTypeName(Class<?> type) {
		return type.getPackage().getName() + "." + type.getSimpleName().substring(1);
	}	
}
