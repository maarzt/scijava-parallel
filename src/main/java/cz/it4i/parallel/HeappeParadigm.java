package cz.it4i.parallel;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.fiji.haas_java_client.HaaSClient;
import cz.it4i.fiji.haas_java_client.SettingsProvider;

@Plugin(type = ParallelizationParadigm.class)
public class HeappeParadigm extends SimpleOstravaParadigm {

	@Parameter
	private int port;

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.HeappeParadigm.class);

	private int numberOfHosts;

	private HaaSClient haasClient;

	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	public void init() {
		if (poolSize == null) {
			poolSize = Math.max(numberOfHosts, 1);
		}
		super.init();
	}
	
	@Override
	public void close() {
		//TODO clean possible open connections
	}

	public void setNumberOfNodes(int number) {
		this.numberOfHosts = number;
	}

	@Override
	protected void initWorkerPool() {

		haasClient = new HaaSClient(SettingsProvider.getSettings(Constants.TEMPLATE_ID, Constants.TIMEOUT,
				Constants.CLUSTER_NODE_TYPE, Constants.PROJECT_ID, Constants.NUMBER_OF_CORE, Constants.CONFIG_FILE_NAME));
		long jobId = haasClient.createJob(Constants.JOB_NAME, numberOfHosts, Collections.emptyList());
		haasClient.submitJob(jobId);
		Collection<String> nodes = getAllocatedNodes(jobId);
		nodes.stream().map(node -> new HeappeWorker(node, port, createConnectionSocketFactory())).forEach(worker -> workerPool.addWorker(worker));
	}

	private ConnectionSocketFactory createConnectionSocketFactory() {
		throw new NotImplementedException("needs to implement in haasClient");
	}

	private Collection<String> getAllocatedNodes(long jobId) {
		// TODO needs to implement in haasClient
		throw new NotImplementedException("needs to implement in haasClient");
	}

}
