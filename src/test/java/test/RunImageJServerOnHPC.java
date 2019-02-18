
package test;

import cz.it4i.parallel.ClusterJobLauncher.Job;
import cz.it4i.parallel.HPCImageJServerRunner;
import cz.it4i.parallel.ui.HPCImageJServerRunnerWithUI;
import net.imagej.ImageJ;

public class RunImageJServerOnHPC {

	public static void main(String[] args) {
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		HPCImageJServerRunner runner = HPCImageJServerRunnerWithUI.gui( ij.context() );
		runner.start();
		Job job = runner.getJob();
		ij.ui().showDialog("Job ID = " + job.getID() + ". Tunnels opened on " +
			runner.getPorts() + " for nodes " + job.getNodes() +
			". Confirm to close.");
	}

}
