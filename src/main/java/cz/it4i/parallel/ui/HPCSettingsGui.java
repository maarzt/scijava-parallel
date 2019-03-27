
package cz.it4i.parallel.ui;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;
import org.scijava.widget.TextWidget;

import cz.it4i.parallel.HPCSettings;

@Plugin(type = Command.class, headless = false)
public class HPCSettingsGui implements Command {

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Host name")
	private String host;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "User name")
	private String userName;

	@Parameter(style = FileWidget.OPEN_STYLE, label = "Key file")
	private File keyFile;

	@Parameter(style = TextWidget.PASSWORD_STYLE, label = "Key file password",
		persist = false, required = false)
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

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Running job ID")
	private String jobID;

	@Parameter(style = TextWidget.FIELD_STYLE,
		label = "Shutdown job when application finishes.")
	private boolean shutdownJobAfterClose;

	@Parameter(type = ItemIO.OUTPUT)
	private HPCSettings settings;

	@Override
	public void run() {
		settings = HPCSettings.builder().host(host).userName(userName).keyFile(
			keyFile).keyFilePassword(keyFilePassword).remoteDirectory(remoteDirectory)
			.command(command).nodes(nodes).ncpus(ncpus).jobID(jobID)
			.shutdownJobAfterClose(shutdownJobAfterClose).build();
	}

	public static HPCSettings showDialog(Context context) {
		CommandService command = context.service(CommandService.class);
		try {
			return (HPCSettings) command.run(HPCSettingsGui.class, true).get()
				.getOutput("settings");
		}
		catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
