
package cz.it4i.parallel.ui;

import java.io.File;
import java.util.function.Consumer;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;
import org.scijava.widget.TextWidget;

import cz.it4i.parallel.HPCImageJServerRunner;

@Plugin(type = Command.class, headless = false)
public class RunImageJServerOnHPCCommand implements Command {

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Host name")
	private String host;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "User name")
	private String userName;

	@Parameter(style = FileWidget.OPEN_STYLE, label = "Key file")
	private File keyFile;

	@Parameter(style = TextWidget.PASSWORD_STYLE, label = "Key file password",
		persist = false)
	private String keyFilePassword;

	// for salomon /scratch/work/project/dd-18-42/apps/fiji-with-server
	@Parameter(style = TextWidget.FIELD_STYLE,
		label = "Remote directory with Fiji")
	private String remoteDirectory;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Remote ImageJ command")
	private String command = "ImageJ-linux64";

	// for salomon run-workers.sh
	@Parameter(style = TextWidget.FIELD_STYLE, label = "Number of nodes")
	private int nodes;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Number of cpus per node")
	private int ncpus;

	@Parameter(required = false)
	private Consumer<HPCImageJServerRunner> runnerConsumer;

	@Parameter(type = ItemIO.OUTPUT)
	private HPCImageJServerRunner runner;

	public void setRunnerConsumer(
		Consumer<HPCImageJServerRunner> runnerConsumer)
	{
		this.runnerConsumer = runnerConsumer;
	}

	@Override
	public void run() {
		runner = new HPCImageJServerRunnerWithUI(host, userName, keyFile,
			keyFilePassword, remoteDirectory, command, nodes, ncpus);
		if (runnerConsumer != null) {
			try (HPCImageJServerRunner localRunner = runner) {
				runnerConsumer.accept(runner);
				runner = null;
			}
		}
	}

}
