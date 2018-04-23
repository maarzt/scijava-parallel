package cz.it4i.parallel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.scijava.command.Command;

import com.google.common.base.Function;

import net.imagej.Dataset;

public class ImageJServerWorker implements ParallelWorker {

	private final String hostName;
	private final int port;
	private Map<Dataset,String> mockedData2id = new HashMap<>();
	private Map<String, Dataset> id2mockedData = new HashMap<>();
	
	
	private final static Set<String> supportedImageTypes = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList("png", "jpg")));

	ImageJServerWorker(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}

	public String getHostName() {
		return hostName;
	}

	public int getPort() {
		return port;
	}

	@Override
	public Dataset importData(Path path) {
		String filePath = path.toAbsolutePath().toString();
		String name = path.getFileName().toString();
		
		String json = null;

		HttpEntity entity = MultipartEntityBuilder.create()
				.addBinaryBody("file", new File(filePath), ContentType.create(getContentType(filePath)), name).build();

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

		String obj = new org.json.JSONObject(json).getString("id");
		Dataset result = Mockito.mock(Dataset.class,(Answer<Dataset>) p->{throw new UnsupportedOperationException();});
		mockedData2id.put(result, obj);
		id2mockedData.put(obj, result);
		return result;
	}

	public void exportData(Dataset dataset, Path p) {
		String filePath = p.toString();
		String id = mockedData2id.get(dataset);
		String getUrl = "http://" + hostName + ":" + String.valueOf(port) + "/objects/" + id + "/" + getImageType(filePath);
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

	public void deleteData(Dataset ds) {
		String id = mockedData2id.get(ds);
		
		@SuppressWarnings("unused")
		String json = null;
	
		String postUrl = "http://" + hostName + ":" + String.valueOf(port) + "/objects/" + id;
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpDelete delete = new HttpDelete(postUrl);
	
		try {
	
			HttpResponse response = httpClient.execute(delete);
			json = EntityUtils.toString(response.getEntity());
			//TODO check result code
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		mockedData2id.remove(ds);
		id2mockedData.remove(id);
	}

	@SuppressWarnings("unchecked")
	public <T extends Command> Map<String, Object> executeCommand(Class<T> commandType, Map<String, ?> inputs) {
		Map<String,Object> map = wrapInputValues(inputs);
		String json = null;

		String postUrl = "http://" + hostName + ":" + String.valueOf(port) + "/modules/" + "command:"
				+ commandType.getCanonicalName();
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(postUrl);

		try {

			JSONObject reqJson = new JSONObject();

			for (Map.Entry<String, ?> pair : map.entrySet()) {
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

		Map<String,Object> result = new HashMap<>();
		org.json.JSONObject jsonObj = new org.json.JSONObject(json);
		for(String key: jsonObj.keySet()) {
			result.put(key, jsonObj.get(key));
		}
		return unwrapOutputValues(result);
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

	public Map<String, String> getArgumentsMap(String commandName) {

		Map<String, String> argumentMap = new HashMap<String, String>();

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

	public Map<String, String> getCommandArgumentsMap(String commandName) {
		return getArgumentsMap("command:" + getCommandByName(commandName));
	}

	// TODO: support another types
	private String getContentType(String path) {
		return "image/" + getImageType(path);
	}
	
	private String getImageType(String path) {
		for (String type : supportedImageTypes) {
			if (path.endsWith("." + type)) {
				return type;
			}
		}

		throw new UnsupportedOperationException("Only " + supportedImageTypes + " image files supported");
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

	private Map<String, Object> wrapInputValues(Map<String, ?> map) {
		return convertMap(map, this::wrapValue);
	}
	
	private Map<String, Object> unwrapOutputValues(Map<String, Object> map) {
		return convertMap(map, this::unwrapValue);
	}


	private Map<String, Object> convertMap(Map<String, ?> map, Function<Object, Object> convertor) {
		return map.entrySet().stream().map(entry -> new P_Entry(entry.getKey(), convertor.apply(entry.getValue())))
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
	}

	
	private Object wrapValue(Object value) {
		if (value instanceof Dataset) {
			Dataset ds = (Dataset) value;
			Object id = mockedData2id.get(ds);
			if(id != null) {
				value = id;
			}
		}
		return value;
	}
	
	private Object unwrapValue(Object value) {
		Dataset obj = id2mockedData.get(value);
		if(obj != null) {
			value = obj;
		}
		return value;
	}
	
	private static class P_Entry implements Map.Entry<String, Object> {

		private final String key;
		private final Object value;
		
		public P_Entry(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public Object setValue(Object value) {
			throw new UnsupportedOperationException();
		}
		
	}
}
