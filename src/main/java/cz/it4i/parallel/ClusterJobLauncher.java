
package cz.it4i.parallel;

import com.jcraft.jsch.JSchException;

import java.io.Closeable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.fiji.scpclient.SshCommandClient;

public class ClusterJobLauncher implements Closeable {

	private final static Logger log = LoggerFactory.getLogger(
		ClusterJobLauncher.class);

	public class Job {

		private String jobId;

		public Job(String jobId) {
			super();
			this.jobId = jobId;
		}

		public void waitForRunning() {

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
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException exc) {
					log.error("waiting", exc);
				}
			}
			while (!(!time.equals("0") && state.equals("R")));
		}

		public List<String> getNodes() {
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
			return new LinkedList<>(new HashSet<>(Arrays.asList(hostLines.stream()
				.collect(Collectors.joining("")).replaceAll(" +", "").replaceAll(
					"exec_host=", "").replaceAll("/[^+]+", "").split("\\+"))));
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

		public void stop() {
			client.executeCommand("qdel " + jobId);
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
		String jobname = new Instant().getMillis() + "";
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

}
