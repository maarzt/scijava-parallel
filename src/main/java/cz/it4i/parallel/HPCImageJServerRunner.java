
package cz.it4i.parallel;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.ClusterJobLauncher.Job;

public class HPCImageJServerRunner extends AbstractImageJServerRunner {

	private final static Logger log = LoggerFactory.getLogger(
		HPCImageJServerRunner.class);

	private String host;

	private String userName;

	private File keyFile;

	private String keyFilePassword;

	private String remoteDirectory;

	private String command = "ImageJ-linux64";

	private int nodes;

	private int ncpus;

	private Job job;

	private ClusterJobLauncher launcher;

	public HPCImageJServerRunner(String host, String userName, File keyFile,
		String keyFilePassword, String remoteDirectory, String command, int nodes,
		int ncpus)
	{
		super();
		this.host = host;
		this.userName = userName;
		this.keyFile = keyFile;
		this.keyFilePassword = keyFilePassword;
		this.remoteDirectory = remoteDirectory;
		this.command = command;
		this.nodes = nodes;
		this.ncpus = ncpus;
		setPorts(Arrays.asList(8080));
	}

	@Override
	public void close() {
		job.stop();
		launcher.close();
	}

	@Override
	protected void doStartImageJServer(List<String> commands) throws IOException {
		launcher = Routines.supplyWithExceptionHandling(
			() -> new ClusterJobLauncher(host, userName, keyFile.toString(),
				keyFilePassword), log, "");
		job = launcher.submit(remoteDirectory, command, commands.subList(1, commands
			.size()).stream().collect(Collectors.joining(" ")), nodes, ncpus);
		job.waitForRunning();
		setPorts(job.createTunnels(8080, 8080));
	}
}
