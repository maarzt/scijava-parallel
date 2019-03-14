package cz.it4i.parallel;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.ClusterJobLauncher.Job;

public class HPCBigDataServerRunTS {

	private final static Logger log = LoggerFactory.getLogger(
		HPCBigDataServerRunTS.class);

	private HPCImageJServerRunner runner;
	private String pathToServer;
	private String pathToDataSet;

	public HPCBigDataServerRunTS(HPCImageJServerRunner runner,
		String pathToServer, String pathToDataSet)
	{
		super();
		this.runner = runner;
		this.pathToServer = pathToServer;
		this.pathToDataSet = pathToDataSet;
	}

	public String run() {
		Job job = runner.getJob();
		int bdsPort = Math.max(Collections.max(runner.getPorts()), 8081) + 1;
		job.createTunnel(bdsPort, "localhost", bdsPort);
		job.runCommandOnNode(0, " -L " + bdsPort + ":localhost:" + bdsPort +
			" " + pathToServer +
			" " + pathToDataSet + " " +
			bdsPort).whenComplete((lines, exc) -> {
				if (exc != null) {
					log.error(exc.getMessage(), exc);
				}
				else {
					log.debug(String.join("\n", lines));
				}
			});
		waitForImageJServer(bdsPort);
		log.debug(
			"BigDataServer is running on node {} and is available local port {}",
			bdsPort, job.getNodes().get(0));
		return "http://localhost:" + bdsPort + "/data";
	}

	private void waitForImageJServer(int port) {
		while (true) {
			try {
				if (checkModulesURL(port)) {
					break;
				}
				Routines.runWithExceptionHandling(() -> Thread.sleep(200));
			}
			catch (IOException e) {
				// ignore waiting for start
			}
		}
	}

	private boolean checkModulesURL(int port) throws IOException {
		HttpURLConnection hc;
		hc = (HttpURLConnection) new URL("http://localhost:" + port + "/json/")
			.openConnection();
		hc.setRequestMethod("GET");
		hc.connect();
		hc.disconnect();
		return hc.getResponseCode() == 200;
	}
}
