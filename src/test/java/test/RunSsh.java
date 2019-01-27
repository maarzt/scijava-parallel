
package test;

import java.io.File;
import java.util.List;
import java.util.Scanner;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;
import org.scijava.widget.TextWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.ClusterJobLauncher;
import cz.it4i.parallel.ClusterJobLauncher.Job;
import cz.it4i.parallel.Routines;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>RunSsh")
public class RunSsh implements Command {

	private final static Logger log = LoggerFactory.getLogger(RunSsh.class);

	public static void main(String[] args) {
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.command().run(RunSsh.class, true);
	}

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Host name")
	private String host;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "User name")
	private String userName;

	@Parameter(style = FileWidget.OPEN_STYLE, label = "Key file")
	private File keyFile;

	@Parameter(style = TextWidget.PASSWORD_STYLE, label = "Key file password",
		persist = false)
	private String keyFilePassword;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Remote directory")
	private String remoteDirectory;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Remote command")
	private String command;

	@Override
	public void run() {
		Routines.runWithExceptionHandling(() -> {
			try (ClusterJobLauncher launcher = new ClusterJobLauncher(host, userName,
				keyFile.toString(), keyFilePassword))
			{
				Job job = launcher.submit(remoteDirectory, command, 8, 12);
				job.waitForRunning();
				List<Integer> nodes = job.createTunnels(8080, 8080);
				log.info("nodes: " + nodes);
				new Scanner(System.in).nextLine();
			}
		}, log, "");

	}

}
