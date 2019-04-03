
package cz.it4i.parallel;

import com.jcraft.jsch.JSchException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.fiji.scpclient.SshCommandClient;
import cz.it4i.fiji.scpclient.SshExecutionSession;

public class ClusterJobLauncher implements Closeable {

	private final static Logger log = LoggerFactory.getLogger(
		ClusterJobLauncher.class);

	private final static long REMOTE_CONSOLE_READ_TIMEOUT = 500;

	public class Job {

		/*
		 * stdout accesible ssh <> ssh <node> tail -f -n +1 /var/spool/PBS/spool/<job id>.OU 
		 */

		private String jobId;

		private CompletableFuture<List<String>> nodesFuture;

		private Job(String jobId) {
			super();
			this.jobId = jobId;
			nodesFuture = CompletableFuture.supplyAsync(() -> getNodesFromServer());
		}

		public CompletableFuture<List<String>> getNodesFuture() {
			return nodesFuture;
		}

		public List<String> getNodes() {
			try {
				return nodesFuture.get();
			}
			catch (InterruptedException | ExecutionException exc) {
				throw new RuntimeException(exc);
			}
		}

		public List<Integer> createTunnels(int startPort, int remotePort) {
			List<Integer> result = new LinkedList<>();
			for (String host : getNodes()) {
				boolean opened;
				do {
					opened = client.setPortForwarding(startPort, host, remotePort);
					if (opened) {
						result.add(startPort);
					}
					startPort++;
				}
				while (!opened);
			}
			return result;
		}

		public CompletableFuture<List<String>> runCommandOnNode(int nodeNumber,
			String command)
		{
			String node = getNodes().get(nodeNumber);
			return CompletableFuture.supplyAsync(() -> client.executeCommand("ssh " +
				node + " " + command));
		}

		public void createTunnel(int localPort, String host, int remotePort) {
			client.setPortForwarding(localPort, host, remotePort);
		}

		public void stop() {
			client.executeCommand("qdel " + jobId);
		}

		public String getID() {
			return jobId;
		}

		private void waitForRunning() {

			if (jobId == null) {
				throw new IllegalStateException("jobId not initialized");
			}
			String state;
			String time;
			do {
				String result = client.executeCommand("qstat " + jobId).get(2);
				String[] tokens = result.split(" +");
				state = tokens[4];
				time = tokens[3];
				sleepForWhile(1000);
			}
			while (!(!time.equals("0") && state.equals("R")));
			new P_OutThread(System.out, "OU").start();
			new P_OutThread(System.err, "ER").start();
		}

		private List<String> getNodesFromServer() {
			waitForRunning();
			if (jobId == null) {
				throw new IllegalStateException("jobId not initialized");
			}
			List<String> result = client.executeCommand("qstat -f " + jobId);
			List<String> hostLines = new LinkedList<>();
			for (String line : result) {
				if (hostLines.isEmpty() && line.contains("exec_host")) {
					hostLines.add(line);
				}
				else if (!hostLines.isEmpty()) {
					if (!line.contains("exec_vnode")) {
						hostLines.add(line);
					}
					else {
						break;
					}
				}
			}
			return new LinkedList<>(new LinkedHashSet<>(Arrays.asList(hostLines
				.stream().collect(Collectors.joining("")).replaceAll(" +", "")
				.replaceAll("exec_host=", "").replaceAll("/[^+]+", "").split("\\+"))));
		}

		private class P_OutThread extends Thread

		{

			private OutputStream outputStream;
			private String suffix;

			public P_OutThread(OutputStream outputStream, String suffix) {
				super();
				this.outputStream = outputStream;
				this.suffix = suffix;
			}

			@Override
			public void run() {
				try (SshExecutionSession session = client.openSshExecutionSession(
					"ssh " + getNodes().get(0) + " tail -f -n +1 /var/spool/PBS/spool/" +
						jobId + "." + suffix))
				{
					byte[] buffer = new byte[1024];
					int readed;
					InputStream is = session.getStdout();
					while (-1 != (readed = is.read(buffer))) {
						outputStream.write(buffer, 0, readed);
						sleepForWhile(REMOTE_CONSOLE_READ_TIMEOUT);
					}
				}
				catch (IOException exc) {
					log.error(exc.getMessage(), exc);
				}
			}
		}
	}

	private SshCommandClient client;

	public ClusterJobLauncher(String hostName, String userName,
		String keyLocation, String keyPassword) throws JSchException
	{
		super();
		this.client = new SshCommandClient(hostName, userName, keyLocation,
			keyPassword);
	}

	public Job submit(String directory, String command, String parameters,
		long usedNodes, long ncpus)
	{
		String jobId = runJob(directory, command, parameters, usedNodes, ncpus);
		return new Job(jobId);
	}

	public Job getSubmittedJob(String jobId) {
		return new Job(jobId);
	}

	@Override
	public void close() {
		this.client.close();
	}

	private String runJob(String directory, String command, String parameters,
		long nodes, long ncpus)
	{
		String jobname = java.time.Instant.now().toEpochMilli() + "";
		String fileName = jobname + ".sh";
// @formatter:off
		String result = client.executeCommand(
			"echo '" + 
			"#!/usr/bin/env bash\n" +
			"cd "+directory+"\n" +
			"pbsdsh -- `readlink -f "+command+"` " + parameters + "\n" +
			"/usr/bin/tail -f /dev/null' > " + fileName + " && " +
			"chmod +x " + fileName +" &&" + 
			"qsub  -q qexp -l select=" + nodes + ":ncpus=" + ncpus + " `readlink -f " + fileName + "`").get(0);
// @formatter:on
		client.executeCommand("rm " + fileName);
		return result;
	}

	static private void sleepForWhile(long timeout) {
		try {
			Thread.sleep(timeout);
		}
		catch (InterruptedException exc) {}
	}

}
