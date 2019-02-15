
package test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import cz.it4i.parallel.ClusterJobLauncher.Job;
import cz.it4i.parallel.ui.RunImageJServerOnHPCCommand;
import cz.it4i.parallel.HPCImageJServerRunner;

@Plugin(type = Command.class, headless = false,
	menuPath = "Plugins>Parallel paradigms>Run ImageJ Server on HPC")
public class RunImageJServerOnHPC implements Command {

	public static void main(String[] args) {
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(RunImageJServerOnHPC.class, true);
	}

	@Parameter
	private UIService uiService;

	@Parameter
	private CommandService commandService;

	@Override
	public void run() {
		Map<String, Object> params = new HashMap<>();
		params.put("runnerConsumer", (Consumer<HPCImageJServerRunner>) runner -> {
			runner.start();
			Job job = runner.getJob();
			uiService.showDialog("Job ID = " + job.getID() + ". Tunnels opened on " +
				runner.getPorts() + " for nodes " + job.getNodes() +
				". Confirm to close.");
		});
		commandService.run(RunImageJServerOnHPCCommand.class, true, params);

	}

}
