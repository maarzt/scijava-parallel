
package test;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.TestParadigm;
import cz.it4i.parallel.ui.RunImageJServerOnHPCCommand;
import net.imagej.ImageJ;
import org.scijava.command.CommandService;
import org.scijava.parallel.ParallelizationParadigm;

import java.util.concurrent.ExecutionException;

public class RotateFileOnHPC {

	public static void main(String[] args) throws ExecutionException, InterruptedException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		final CommandService commandService = ij.command();

		AbstractImageJServerRunner runner = ( AbstractImageJServerRunner ) commandService.run(
			RunImageJServerOnHPCCommand.class, true).get().getOutputs().get( "runner" );

		try(ParallelizationParadigm paradigm = new TestParadigm( runner, ij.context() )) {
			RotateFile.callRemotePlugin(paradigm);
		}
	}
}
