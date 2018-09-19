
package cz.it4i.parallel;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
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

	private Long jobId;

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
		jobId = haasClient.createJob(new JobSettingsBuilder().templateId(
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
		while (logGetState() == JobState.Queued) {
			try {
				Thread.sleep(TIMEOUT_BETWEEN_JOB_STATE_QUERY);
			}
			catch (final InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}
		final JobState state = logGetState();
		if (state == JobState.Running) {
			final Collection<String> nodes = getAllocatedNodes();
			nodes.stream().map(node -> {
				try (CloseableHttpClient client = HttpClientBuilder.create()
					.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
					.build())
				{
					do {
						TunnelToNode tunnel;
						tunnel = haasClient.openTunnel(jobId, node, 0, port);
						if (!checkTunnel2ImageJServer(client, tunnel)) {
							try {
								tunnel.close();
								Thread.sleep(5000);
							}
							catch (IOException | InterruptedException exc) {
								log.error("Restart tunnel", exc);
							}
							continue;
						}
						tunnels.add(tunnel);
						return new HeappeWorker(tunnel.getLocalHost(), tunnel.getLocalPort());
					} while(true);
				}
				catch (IOException exc1) {
					throw new RuntimeException(exc1);
				}
			}).forEach(worker -> workerPool.addWorker(worker));
		}
		else {
			log.error("Job ID: {} not running. It is in state {}.", jobId, state
				.toString());
		}
	}

	private boolean checkTunnel2ImageJServer(CloseableHttpClient client, TunnelToNode tunnel) {
		HttpGet httpGet = new HttpGet("http://"+tunnel.getLocalHost() + ":" + tunnel.getLocalPort() + "/modules");
		
		HttpResponse response;
		try {
			response = client.execute(httpGet);
			if (log.isDebugEnabled()) {
				log.debug(response.toString());
			}
			return true;
		}
		catch (IOException exc) {
			log.debug("modules", exc);
		}
	
		return false;
	}

	// -- Closeable methods --
	@Override
	public void close() {
		super.close();
		deleteJob();
		closeTunnels();
	}

	// -- Helper methods --
	private Collection<String> getAllocatedNodes() {
		return haasClient.obtainJobInfo(jobId).getNodesIPs();
	}

	private JobState logGetState() {
		final JobState result = haasClient.obtainJobInfo(jobId).getState();
		if (log.isDebugEnabled()) {
			log.debug("state of job " + jobId + " - " + result);
		}
		return result;
	}

	private void deleteJob() {
		if (jobId != null) {
			if (haasClient.obtainJobInfo(jobId).getState() == JobState.Running) {
				runAndLogIfThrowsException("Cancel job " + jobId, () -> haasClient.cancelJob(jobId));
			}
			runAndLogIfThrowsException("Delete job " + jobId, () -> haasClient.deleteJob(jobId)); 
		}
		jobId = null;
	}

	private void runAndLogIfThrowsException(String message, P_Runnable runnable) {
		try {
			runnable.run();
		} catch (Exception e) {
			log.error(message, e);
		}
		
	}

	private void closeTunnels() {
		tunnels.forEach(t -> {
			try {
				t.close();
			}
			catch (final IOException e) {
				log.error(e.getMessage(), e);
			}
		});
	}

	private interface P_Runnable {
		void run () throws Exception;
	}
}
