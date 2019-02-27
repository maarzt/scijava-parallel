
package cz.it4i.parallel;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cz.it4i.parallel.ClusterJobLauncher.Job;

public class HPCImageJServerRunner extends AbstractImageJServerRunner {

	private List< Integer > ports;

	private final HPCSettings settings;

	private Job job;

	private ClusterJobLauncher launcher;

	public HPCImageJServerRunner( HPCSettings settings )
	{
		super();
		this.settings = settings;
		this.ports = Collections.emptyList();
	}

	public Job getJob() {
		return job;
	}

	@Override
	public void close() {
		if (job != null) {
			job.stop();
		}
		launcher.close();
	}

	@Override
	protected void doStartImageJServer() throws IOException {
		launcher = Routines.supplyWithExceptionHandling(
			() -> new ClusterJobLauncher(settings.getHost(), settings.getUserName(), settings.getKeyFile().toString(),
				settings.getKeyFilePassword()));
		final String arguments = AbstractImageJServerRunner.IMAGEJ_SERVER_PARAMETERS
				.stream().collect( Collectors.joining( " " ) );
		job = launcher.submit(settings.getRemoteDirectory(), settings.getCommand(), arguments,
				settings.getNodes(), settings.getNcpus());
		ports = job.createTunnels(8080, 8080);
	}

	@Override
	public List< Integer > getPorts()
	{
		return ports;
	}
}
