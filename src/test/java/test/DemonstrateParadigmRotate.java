
package test;

import static test.Config.JPG_SUFFIX;
import static test.Config.PNG_SUFFIX;
import static test.Config.getInputDirectory;
import static test.Config.getOutputFilesPattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigmProfile;
import org.scijava.parallel.RemoteDataset;
import org.scijava.parallel.WritableDataset;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.HeappeParadigm;
import cz.it4i.parallel.ImageJServerParadigm;
import cz.it4i.parallel.LocalMultithreadedParadigm;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>DemonstrateOstravaParadigm")
public class DemonstrateParadigmRotate implements Command {

	private static int step;
	
	// ImageJServerParadigm-specific stuff
	private static List<String> hosts = new LinkedList<>();

	// LocalMultithreadedParadigm-specific stuff
	private static int numberOfLocalWorkers;

	// HeappeParadigm-specific stuff
	private static int remotePort;
	private static int numberOfNodes;

	public static final Logger log = LoggerFactory.getLogger(
		DemonstrateParadigmRotate.class);

	@Parameter
	private ParallelService parallelService;

	public static void main(final String... args) {

		final Iterator<String> argIter = Arrays.asList(args).iterator();
		argIter.next();
		step = Integer.parseInt(argIter.next());

		if (args[0].equals("-s")) {
			while (argIter.hasNext()) {
				hosts.add(argIter.next());
			}
		}
		else if (args[0].equals("-l")) {
			numberOfLocalWorkers = Integer.parseInt(argIter.next());
		}
		else if (args[0].equals("-h")) {
			remotePort = Integer.parseInt(argIter.next());
			numberOfNodes = Integer.parseInt(argIter.next());
		}
		else {
			log.error("Invalid input arguments.");
			return;
		}

		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrateParadigmRotate.class, true);
	}

	@Override
	public void run() {

		// Uncomment to see the previously saved profiles
		// @SuppressWarnings("unused")
		// List<ParallelizationParadigmProfile> profiles = parallelService
		// .getProfiles();

		// Remove all saved profiles
		parallelService.deleteProfiles();

		// Add few profiles
		parallelService.addProfile(new ParallelizationParadigmProfile(
			ImageJServerParadigm.class, "lonelyBiologist01"));
		parallelService.addProfile(new ParallelizationParadigmProfile(
			LocalMultithreadedParadigm.class, "lonelyBiologist02"));
		parallelService.addProfile(new ParallelizationParadigmProfile(
			HeappeParadigm.class, "lonelyBiologist03"));

		// Set one of the profiles to be used
		if (hosts.size() > 0) {
			parallelService.selectProfile("lonelyBiologist01");
		}
		else if (numberOfNodes > 0) {
			parallelService.selectProfile("lonelyBiologist03");
		}
		else {
			parallelService.selectProfile("lonelyBiologist02");
		}

		// Retrieve the paradigm
		try (ParallelizationParadigm paradigm = parallelService.getParadigm()) {

			// Init the paradigm and do the tests
			if (hosts.size() > 0) {
				((ImageJServerParadigm) paradigm).setHosts(hosts);
				paradigm.init();

				doTest(paradigm);
			}
			else if (numberOfNodes > 0) {
				((HeappeParadigm) paradigm).setPort(remotePort);
				((HeappeParadigm) paradigm).setNumberOfNodes(numberOfNodes);
				((HeappeParadigm) paradigm).init();
				doTest(paradigm);
			}
			else {
				((LocalMultithreadedParadigm) paradigm).setPoolSize(numberOfLocalWorkers);
				doTest(paradigm);
			}
		}
	}

	private void doTest(final ParallelizationParadigm paradigm) {
		Path file;
		try {
			file = Files.newDirectoryStream(Paths.get(getInputDirectory()), p -> p
				.toString().endsWith(PNG_SUFFIX) || p.toString().endsWith(JPG_SUFFIX))
				.iterator().next();
		}
		catch (IOException exc) {
			log.error("doTest", exc);
			throw new RuntimeException(exc);
		}

		List<Class<? extends Command>> commands = new LinkedList<>();
		List<Map<String, ?>> paramsList = new LinkedList<>();
		for (double angle = step; angle < 360; angle += step) {
			commands.add(RotateImageXY.class);
			Map<String, Object> params = new HashMap<>();
			RemoteDataset rd = paradigm.createRemoteDataset(file.toUri());
			params.put("dataset", rd);
			params.put("angle", angle);
			paramsList.add(params);
		}
		List<Map<String, ?>> results = paradigm.runAll(commands, paramsList);
		int angle = step;
		for (Map<String,?> result: results) {
			paradigm.exportWritableDatased((WritableDataset) result.get("dataset"), Paths.get(getOutputFilesPattern() + angle + ".png").toUri());
			angle += step;
		}
		log.info("result: " + results);

	}

}
