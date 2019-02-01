
package test;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.ui.RunImageJServerOnHPCCommand;

@Plugin(type = Command.class, headless = false)
public class RotateFileOnHPC extends RotateFileAsync {

	@Parameter
	private CommandService commandService;

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(RotateFileOnHPC.class, true);
	}

	@Override
	protected AbstractImageJServerRunner constructImageJServerRunner() {
		try {
			Map<String, Object> result = commandService.run(
				RunImageJServerOnHPCCommand.class, true).get().getOutputs();
			return (AbstractImageJServerRunner) result.get("runner");
		}
		catch (InterruptedException | ExecutionException exc) {
			throw new RuntimeException(exc);
		}

	}

}
