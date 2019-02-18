
package test;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.TestParadigm;
import cz.it4i.parallel.ui.HPCImageJServerRunnerWithUI;
import net.imagej.ImageJ;
import org.scijava.parallel.ParallelizationParadigm;

import java.util.concurrent.ExecutionException;

public class RotateFileOnHPC {

	public static void main(String[] args) throws ExecutionException, InterruptedException
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		AbstractImageJServerRunner runner = HPCImageJServerRunnerWithUI.gui( ij.context() );

		try(ParallelizationParadigm paradigm = new TestParadigm( runner, ij.context() )) {
			RotateFile.callRemotePlugin(paradigm);
		}
	}
}
