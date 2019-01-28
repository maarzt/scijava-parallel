
package cz.it4i.parallel;

import static cz.it4i.parallel.Routines.supplyWithExceptionHandling;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractImageJServerParadigm extends
	SimpleOstravaParadigm
{

	public static final Logger log = LoggerFactory.getLogger(
		AbstractImageJServerParadigm.class);

	private ParameterTypeProvider typeProvider = null;

	@Override
	protected void initWorkerPool() {
		getHosts().forEach(host -> workerPool.addWorker(createWorker(host)));
	}

	private ParallelWorker createWorker(String host) {
		final String[] tokensOfHost = host.split(":");
		int port = Integer.parseInt(tokensOfHost[1]);
		host = tokensOfHost[0];
		synchronized (this) {
			if (typeProvider == null) {
				typeProvider = new P_ParameterTypeProvider(port, host);
			}
		}
		return new ImageJServerWorker(host, port);
	}

	@Override
	protected ParameterTypeProvider getTypeProvider() {
		return typeProvider;
	}

	abstract protected Collection<String> getHosts();

	private class P_ParameterTypeProvider implements ParameterTypeProvider {

		private Map<String, Map<String, String>> mappedTypes = new HashMap<>();

		private int port;
		private String host;

		public P_ParameterTypeProvider(int port, String host) {
			super();
			this.port = port;
			this.host = host;
		}

		@Override
		public String provideParameterTypeName(String commandName,
			String parameterName)
		{
			Map<String, String> paramToClass = mappedTypes.computeIfAbsent(
				commandName, c -> obtainCommandInfo(c));
			return paramToClass.get(parameterName);
		}

		private Map<String, String> obtainCommandInfo(String commandTypeName) {
			Map<String, String> result = new HashMap<>();
			final String getUrl = "http://" + host + ":" + String.valueOf(port) +
				"/modules/" + "command:" + commandTypeName;
			final HttpGet httpGet = new HttpGet(getUrl);
			final HttpResponse response = supplyWithExceptionHandling(
				() -> HttpClientBuilder.create().build().execute(httpGet), log,
				"get response");
			org.json.JSONObject json = supplyWithExceptionHandling(
				() -> new org.json.JSONObject(EntityUtils.toString(response
					.getEntity())), log, "obtain command info");

			org.json.JSONArray inputs = (org.json.JSONArray) json.get("inputs");
			Iterator<?> iter = inputs.iterator();
			while (iter.hasNext()) {
				org.json.JSONObject param = (org.json.JSONObject) iter.next();
				String typeName = ((String) param.get("genericType")).trim();
				if (typeName.contains(" ")) {
					typeName = typeName.split(" ")[1];
				}
				if (Character.isLowerCase(typeName.charAt(0)) && !typeName.contains(
					"."))
				{
					typeName = "java.lang." + Character.toUpperCase(typeName.charAt(0)) +
						typeName.substring(1);
				}
				result.put((String) param.get("name"), typeName);
			}
			return result;
		}

	}
}
