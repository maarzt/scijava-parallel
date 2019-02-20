
package test;

import net.imagej.ImageJ;

import org.scijava.parallel.ParallelizationParadigm;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.TestParadigm;
import cz.it4i.parallel.ui.HPCImageJServerRunnerWithUI;

public class RotateFileOnHPC {

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		AbstractImageJServerRunner runner = HPCImageJServerRunnerWithUI.gui( ij.context() );

		try(ParallelizationParadigm paradigm = new TestParadigm( runner, ij.context() )) {
			RotateFile.callRemotePlugin(paradigm);
		}
	}
}
