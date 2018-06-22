package cz.it4i.parallel;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.fiji.haas_java_client.HaaSClient;
import cz.it4i.fiji.haas_java_client.JobState;
import cz.it4i.fiji.haas_java_client.SettingsProvider;
import cz.it4i.fiji.haas_java_client.TunnelToNode;

@Plugin(type = ParallelizationParadigm.class)
public class HeappeParadigm extends SimpleOstravaParadigm {

	private static final int TIMEOUT_BETWEEN_JOB_STATE_QUERY = 1000;

	@Parameter
	private int port;

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.HeappeParadigm.class);

	private int numberOfHosts;

	private HaaSClient haasClient;
	private final Collection<TunnelToNode> tunnels = Collections.synchronizedList(new LinkedList<>());
	
	// -- HeappeParadigm methods --

	public void setPort(int port) {
		this.port = port;
	}
	
	public void setNumberOfNodes(int number) {
		this.numberOfHosts = number;
	}

	// -- SimpleOstravaParadigm methods --
	
	@Override
	protected void initWorkerPool() {
		if (log.isDebugEnabled()) {
			log.debug("initWorkerPool");
		}
		haasClient = new HaaSClient(SettingsProvider.getSettings(Constants.TEMPLATE_ID, Constants.TIMEOUT,
				Constants.CLUSTER_NODE_TYPE, Constants.PROJECT_ID, Constants.NUMBER_OF_CORE, Constants.CONFIG_FILE_NAME));
		if (log.isDebugEnabled()) {
			log.debug("createJob");
		}
		long jobId = haasClient.createJob(Constants.JOB_NAME, numberOfHosts, Collections.emptyList());
		if (log.isDebugEnabled()) {
			log.debug("submitJob");
		}
		haasClient.submitJob(jobId);
		while(logGetState(jobId) == JobState.Queued) {
			try {
				Thread.sleep(TIMEOUT_BETWEEN_JOB_STATE_QUERY);
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}
		Collection<String> nodes = getAllocatedNodes(jobId);
		nodes.stream().map(node -> 
		{	
			TunnelToNode tunnel;
			tunnels.add(tunnel = haasClient.openTunnel(jobId, node, 0, port));
			return new HeappeWorker(tunnel.getLocalHost(), tunnel.getLocalPort());
		}).forEach(worker -> workerPool.addWorker(worker));
	}
	
	private JobState logGetState(long jobId) {
		JobState result = haasClient.obtainJobInfo(jobId).getState();
		if (log.isDebugEnabled()) {
			log.debug("state of job " + jobId + " - " + result);
		}
		return result;
	}

	// -- Closeable methods --
	
	@Override
	public void close() {
		tunnels.forEach(t->{
			try {
				t.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		});
	}
	
	// -- Helper methods --
	
	

	private Collection<String> getAllocatedNodes(long jobId) {
		return haasClient.obtainJobInfo(jobId).getNodesIPs();
	}

}
