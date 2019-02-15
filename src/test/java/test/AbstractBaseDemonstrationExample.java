
package test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.imagej.ImageJ;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigmProfile;
import org.scijava.plugin.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.ImageJServerParadigm;
import cz.it4i.parallel.ImageJServerRunner;

abstract public class AbstractBaseDemonstrationExample implements Command {

	private List<String> hosts = Arrays.asList("localhost:8080");

	private static final Logger log = LoggerFactory.getLogger(
		AbstractBaseDemonstrationExample.class);

	@Parameter
	private ParallelService parallelService;

	@Parameter
	private Context context;

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(AbstractBaseDemonstrationExample.class, true);
	}

	@Override
	public void run() {
		try {
			try (AbstractImageJServerRunner imageJServerRunner =
				constructImageJServerRunner())
			{
				imageJServerRunner.start();
				hosts = imageJServerRunner.getPorts().stream().map(
					port -> "localhost:" + port).collect(Collectors.toList());
				try (ParallelizationParadigm paradigm = configureParadigm()) {
					callRemotePlugin(paradigm);
				}

			}
		}
		catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
		finally {
			System.exit(0);
		}
	}

	protected AbstractImageJServerRunner constructImageJServerRunner() {
		return new ImageJServerRunner(Config.getFijiExecutable());
	}

	abstract protected void callRemotePlugin(
		final ParallelizationParadigm paradigm);

	private ParallelizationParadigm configureParadigm() {
		parallelService.deleteProfiles();
		parallelService.addProfile(new ParallelizationParadigmProfile(
			ImageJServerParadigm.class, "lonelyBiologist01"));
		parallelService.selectProfile("lonelyBiologist01");

		ParallelizationParadigm paradigm = parallelService.getParadigm();
		((ImageJServerParadigm) paradigm).setHosts(hosts);
		paradigm.init();
		return paradigm;
	}

}
