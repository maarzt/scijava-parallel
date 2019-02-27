
package cz.it4i.parallel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.scijava.Context;
import org.scijava.plugin.SciJavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageJServerWorker implements ParallelWorker {

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.ImageJServerWorker.class);

	private final String hostName;
	private final int port;

	private final static Set<String> supportedImageTypes = Collections
		.unmodifiableSet(new HashSet<>(Arrays.asList("png", "jpg")));

	private final Map<org.json.JSONObject, String> importedData2id =
		new HashMap<>();
	private final Map<String, org.json.JSONObject> id2importedData =
		new HashMap<>();

	ImageJServerWorker(final String hostName, final int port) {
		this.hostName = hostName;
		this.port = port;
	}

	public String getHostName() {
		return hostName;
	}

	public int getPort() {
		return port;
	}

	// -- ParallelWorker methods --

	@Override
	public org.json.JSONObject importData(final Path path) {

		final String filePath = path.toAbsolutePath().toString();
		final String fileName = path.getFileName().toString();
		return importData(new FileBody(new File(filePath), ContentType.create(
			getContentType(filePath)), fileName));
	}

	public org.json.JSONObject importData(final String fileName, long length,
		Consumer<OutputStream> osConsumer)
	{

		return importData(new AbstractContentBody(ContentType.create(getContentType(
			fileName)))
		{

			@Override
			public String getTransferEncoding() {
				return MIME.ENC_BINARY;
			}

			@Override
			public long getContentLength() {
				return length;
			}

			@Override
			public void writeTo(OutputStream out) throws IOException {
				osConsumer.accept(out);
			}

			@Override
			public String getFilename() {
				return fileName;
			}
		});
	}

	@Override
	public void exportData(final Object dataset, final Path p) {

		final String filePath = p.toString();
		final String fileName = p.getFileName().toString();
		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(
			new File(filePath))))
		{
			exportData(dataset, fileName, new Consumer<InputStream>() {

				@Override
				public void accept(InputStream t) {
					int inByte;
					try {
						while ((inByte = t.read()) != -1) {
							os.write(inByte);
						}
					}
					catch (IOException exc) {
						log.error("", exc);
					}
				}
			});
		}
		catch (final Exception e) {
			log.error("", e);
		}
	}

	public void exportData(final Object dataset, final String filePath,
		Consumer<InputStream> isConsumer) throws IOException,
		ClientProtocolException
	{
		final String objectId = ((org.json.JSONObject) dataset).getString("id");
		final String getUrl = "http://" + hostName + ":" + String.valueOf(port) +
			"/objects/" + objectId + "/" + getImageType(filePath);
		final HttpGet httpGet = new HttpGet(getUrl);

		final HttpEntity entity = HttpClientBuilder.create().build().execute(
			httpGet).getEntity();

		if (entity != null) {

			try (BufferedInputStream bis = new BufferedInputStream(entity
				.getContent()))
			{
				isConsumer.accept(bis);
			}
		}
	}

	@Override
	public void deleteData(final Object dataset) {

		final String objectId = ((org.json.JSONObject) dataset).getString("id");

		@SuppressWarnings("unused")
		String json = null;

		try {

			final String postUrl = "http://" + hostName + ":" + String.valueOf(port) +
				"/objects/" + objectId;
			final HttpDelete httpDelete = new HttpDelete(postUrl);

			final HttpResponse response = HttpClientBuilder.create().build().execute(
				httpDelete);

			// TODO check result code properly

			json = EntityUtils.toString(response.getEntity());

		}
		catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @throws RuntimeException if response from the ImageJ server is not successful, or json cannot be parsed properly.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> executeCommand(final String commandTypeName,
		final Map<String, ?> inputs)
	{

		final Map<String, ?> wrappedInputs = wrapInputValues(inputs);

		try {

			final String postUrl = "http://" + hostName + ":" + String.valueOf(port) +
				"/modules/" + "command:" + commandTypeName;
			final HttpPost httpPost = new HttpPost(postUrl);

			final JSONObject inputJson = new JSONObject();

			for (final Map.Entry<String, ?> pair : wrappedInputs.entrySet()) {
				inputJson.put(pair.getKey(), pair.getValue());
			}

			httpPost.setEntity(new StringEntity(inputJson.toString()));
			httpPost.setHeader("Content-type", "application/json");

			final HttpResponse response = HttpClientBuilder.create().build().execute(
				httpPost);

			int statusCode = response.getStatusLine().getStatusCode();
			boolean success = Response.Status.fromStatusCode(statusCode).getFamily() == Response.Status.Family.SUCCESSFUL;
			if ( !success ) {
				throw new RuntimeException( "Command cannot be executed" + response.getStatusLine() + " " + response.getEntity() );
			}

			String json = EntityUtils.toString( response.getEntity() );

			final Map<String, Object> rawOutputs = new HashMap<>();

			final org.json.JSONObject jsonObj = new org.json.JSONObject(json);

			jsonObj.keys().forEachRemaining(key -> rawOutputs.put(key, jsonObj.get(
				key)));

			return unwrapOutputValues(rawOutputs);
		}
		catch ( IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	// -- Helper methods --

	private org.json.JSONObject importData(ContentBody contentBody) {
		return Routines.supplyWithExceptionHandling(() -> {

			final String postUrl = "http://" + hostName + ":" + String.valueOf(port) +
				"/objects/upload";
			final HttpPost httpPost = new HttpPost(postUrl);

			final HttpEntity entity = MultipartEntityBuilder.create().addPart("file",
				contentBody).build();
			httpPost.setEntity(entity);

			final HttpResponse response = HttpClientBuilder.create().build().execute(
				httpPost);

			// TODO check result code properly

			final String json = EntityUtils.toString(response.getEntity());
			org.json.JSONObject result = new org.json.JSONObject(json);
			final String objId = result.getString("id");

			importedData2id.put(result, objId);
			id2importedData.put(objId, result);
			return result;
		});
	}

	// TODO: support another types
	private String getContentType(final String path) {
		return "image/" + getImageType(path);
	}

	private String getImageType(final String path) {
		for (final String type : supportedImageTypes) {
			if (path.endsWith("." + type)) {
				return type;
			}
		}

		throw new UnsupportedOperationException("Only " + supportedImageTypes +
			" image files supported");
	}

	private Map<String, Object> wrapInputValues(final Map<String, ?> map) {
		return convertMap(map, ImageJServerWorker::isEntryResolvable,
			this::wrapValue);
	}

	private Map<String, Object> unwrapOutputValues(
		final Map<String, Object> map)
	{
		return convertMap(map, ImageJServerWorker::isEntryResolvable,
			this::unwrapValue);
	}

	/**
	 * Converts an input map into an output map
	 * 
	 * @param map - an input map
	 * @param filter - a filter to be applied on all map entries prior the actual
	 *          conversion
	 * @param converter - a converter to be applied on each map entry
	 * @return a converted map
	 */
	private Map<String, Object> convertMap(final Map<String, ?> map,
		final Function<Map.Entry<String, ?>, Boolean> filter,
		final Function<Object, Object> converter)
	{
		return map.entrySet().stream().filter(entry -> filter.apply(entry)).map(
			entry -> new SimpleImmutableEntry<>(entry.getKey(), converter.apply(entry
				.getValue()))).collect(Collectors.toMap(e -> e.getKey(), e -> e
					.getValue()));
	}

	private Object wrapValue(Object value) {
		if (value instanceof org.json.JSONObject) {
			final Object id = importedData2id.get(value);
			if (id != null) {
				value = id;
			}
		}
		return value;
	}

	private Object unwrapValue(Object value) {
		final Object obj = id2importedData.get(value);
		if (obj != null) {
			value = obj;
		}
		return value;
	}

	/**
	 * Determines whether an entry is resolvable from the SciJava Context
	 */
	private static boolean isEntryResolvable(final Map.Entry<String, ?> entry) {
		return entry.getValue() != null && !(entry
			.getValue() instanceof SciJavaPlugin) && !(entry
				.getValue() instanceof Context);
	}
}
