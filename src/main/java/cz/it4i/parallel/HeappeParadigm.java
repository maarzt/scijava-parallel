
package cz.it4i.parallel;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.fiji.haas_java_client.HaaSClient;
import cz.it4i.fiji.haas_java_client.HaaSFileTransfer;
import cz.it4i.fiji.haas_java_client.JobSettingsBuilder;
import cz.it4i.fiji.haas_java_client.JobState;
import cz.it4i.fiji.haas_java_client.SettingsProvider;
import cz.it4i.fiji.haas_java_client.TunnelToNode;
import cz.it4i.fiji.haas_java_client.UploadingFileData;

@Plugin(type = ParallelizationParadigm.class)
public class HeappeParadigm extends SimpleOstravaParadigm {

	

	private static final int TIMEOUT_BETWEEN_JOB_STATE_QUERY = 1000;

	private static final Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.HeappeParadigm.class);

	@Parameter
	private int port;

	@Parameter
	private int numberOfHosts;

	private HaaSClient haasClient;
	private final Collection<TunnelToNode> tunnels = Collections.synchronizedList(
		new LinkedList<>());

	// -- HeappeParadigm methods --

	public void setPort(final int port) {
		this.port = port;
	}

	public void setNumberOfNodes(final int number) {
		this.numberOfHosts = number;
	}

	// -- SimpleOstravaParadigm methods --

	// -- Init methods --
	@Override
	protected void initWorkerPool() {
		if (log.isDebugEnabled()) {
			log.debug("initWorkerPool");
		}
		haasClient = new HaaSClient(SettingsProvider.getSettings(
			Constants.PROJECT_ID, Constants.CONFIG_FILE_NAME));
		if (log.isDebugEnabled()) {
			log.debug("createJob");
		}
		final long jobId = haasClient.createJob(new JobSettingsBuilder().templateId(
			Constants.HEAppE.TEMPLATE_ID).walltimeLimit(Constants.WALLTIME)
			.clusterNodeType(Constants.CLUSTER_NODE_TYPE).jobName(
				Constants.HEAppE.JOB_NAME).numberOfNodes(numberOfHosts)
			.numberOfCoresPerNode(Constants.NUMBER_OF_CORE).build(), Collections
				.emptyList());
		if (log.isDebugEnabled()) {
			log.debug("submitJob");
		}
		try(HaaSFileTransfer hft = haasClient.startFileTransfer(jobId)) {
			hft.upload(new UploadingFileData(Constants.HEAppE.RUN_IJS));
		}
		catch (InterruptedIOException exc) {
			log.error(exc.getMessage(), exc);
		}
		haasClient.submitJob(jobId);
		while (logGetState(jobId) == JobState.Queued) {
			try {
				Thread.sleep(TIMEOUT_BETWEEN_JOB_STATE_QUERY);
			}
			catch (final InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}
		final JobState state = logGetState(jobId);
		if (state == JobState.Running) {
			final Collection<String> nodes = getAllocatedNodes(jobId);
			nodes.stream().map(node -> {
				TunnelToNode tunnel;
				tunnels.add(tunnel = haasClient.openTunnel(jobId, node, 0, port));
				return new HeappeWorker(tunnel.getLocalHost(), tunnel.getLocalPort());
			}).forEach(worker -> workerPool.addWorker(worker));
		}
		else {
			log.error("Job ID: %d not running. It is in state %s.", jobId, state
				.toString());
		}
	}

	// -- Closeable methods --
	@Override
	public void close() {
		tunnels.forEach(t -> {
			try {
				t.close();
			}
			catch (final IOException e) {
				log.error(e.getMessage(), e);
			}
		});
	}

	// -- Helper methods --
	private Collection<String> getAllocatedNodes(final long jobId) {
		return haasClient.obtainJobInfo(jobId).getNodesIPs();
	}

	private JobState logGetState(final long jobId) {
		final JobState result = haasClient.obtainJobInfo(jobId).getState();
		if (log.isDebugEnabled()) {
			log.debug("state of job " + jobId + " - " + result);
		}
		return result;
	}

}
