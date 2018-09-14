
package test.learnathon;

import static test.Config.getResultFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedList;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigmProfile;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.HeappeParadigm;
import cz.it4i.parallel.ImageJServerParadigm;
import cz.it4i.parallel.LocalMultithreadedParadigm;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>ParadigmDemonstration")
public class Demonstration implements Command {

	private static int step = 10;
	private static int repetitionCount = 10;
	private static int maxNumberOfLocalWorkers;

	public static final Logger log = LoggerFactory.getLogger(Demonstration.class);

	public static void main(final String... args) {
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.command().run(Demonstration.class, true);
	}

	@Parameter
	private ParallelService parallelService;

	@Override
	public void run() {

		// Uncomment to see the previously saved profiles
		// @SuppressWarnings("unused")
		// List<ParallelizationParadigmProfile> profiles = parallelService
		// .getProfiles();

		// Remove all saved profiles
		parallelService.deleteProfiles();

		// Add a few profiles
		parallelService.addProfile(new ParallelizationParadigmProfile(
			ImageJServerParadigm.class, "lonelyBiologist01"));
		parallelService.addProfile(new ParallelizationParadigmProfile(
			LocalMultithreadedParadigm.class, "lonelyBiologist02"));
		parallelService.addProfile(new ParallelizationParadigmProfile(
			HeappeParadigm.class, "lonelyBiologist03"));

		// Prepare inputs
		final Collection<Integer> angles = new LinkedList<>();
		final Path fileToRotate = Paths.get("/tmp/input/heart.png");
		for (int angle = step; angle < 360; angle += step) {
			angles.add(angle);
		}

		// Set one of the profiles to be used
		parallelService.selectProfile("lonelyBiologist01");

		// Specific set-up
		ParallelizationParadigm paradigm = parallelService.getParadigm();
		final Collection<String> hosts = new LinkedList<>();
		hosts.add("localhost:8080");
		((ImageJServerParadigm) paradigm).setHosts(hosts);

		// Common initialization
		paradigm.init();

		// Do it!
		doTest(paradigm, angles, fileToRotate, hosts.size());

		// Repeat
		parallelService.selectProfile("lonelyBiologist02");
		paradigm = parallelService.getParadigm();

		for (int numberOfWorkers =
			maxNumberOfLocalWorkers; 0 < numberOfWorkers; numberOfWorkers--)
		{
			((LocalMultithreadedParadigm) paradigm).setPoolSize(numberOfWorkers);
			paradigm.init();
			doTest(paradigm, angles, fileToRotate, numberOfWorkers);
		}

	}

	private void doTest(final ParallelizationParadigm paradigm,
		final Collection<Integer> angles, final Path fileToRotate,
		final int numberOfWorkers)
	{

		log.info("Number of workers: " + numberOfWorkers);
		for (int i = 0; i < repetitionCount; i++) {
			final long startTime = System.currentTimeMillis();
			paradigm.parallelFor(angles, (angle, executionContext) -> {
				log.debug("Processing angle=" + angle);
				Dataset ds = executionContext.importData(fileToRotate);
				final RotateImageXY<?> command = executionContext.getRemoteCommand(
					RotateImageXY.class);
				command.setAngle(angle);
				command.setDataset(ds);
				command.run();
				ds = command.getDataset();
				executionContext.exportData(ds, Paths.get("/tmp/output/result_" +
					angle + ".jpg"));
				log.debug("DONE: processing angle=" + angle);
			});
			final long endTime = System.currentTimeMillis();
			final double timeNeededInSec = (endTime - startTime) / 1000.;
			final String resultStr = "Number of workers: " + numberOfWorkers +
				", time: " + timeNeededInSec;
			writeResult(resultStr);
			log.info("Done iteration: " + resultStr);
		}
	}

	private void writeResult(final String resultStr) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(getResultFile()),
			StandardOpenOption.APPEND, StandardOpenOption.CREATE))
		{
			bw.write(resultStr + "\n");
		}
		catch (final IOException e) {
			log.error(e.getMessage(), e);
		}
	}
}
