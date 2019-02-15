
package test;

import cz.it4i.parallel.TestParadigm;
import net.imagej.ImageJ;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.ImageJServerRunner;

abstract public class AbstractBaseDemonstrationExample implements Command {

	private static final Logger log = LoggerFactory.getLogger(
		AbstractBaseDemonstrationExample.class);
	@Parameter
	private Context context;

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(AbstractBaseDemonstrationExample.class, true);
	}

	@Override
	public void run() {
		try ( ParallelizationParadigm paradigm = new TestParadigm( constructImageJServerRunner(), context ) ) {
			callRemotePlugin(paradigm);
		}
	}

	protected AbstractImageJServerRunner constructImageJServerRunner() {
		return new ImageJServerRunner(Config.getFijiExecutable());
	}

	abstract protected void callRemotePlugin(
		final ParallelizationParadigm paradigm);

}
