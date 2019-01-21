
package test;

import static test.Config.JPG_SUFFIX;
import static test.Config.PNG_SUFFIX;
import static test.Config.getInputDirectory;
import static test.Config.getOutputFilesPattern;
import static test.Config.getResultFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.imagej.ImageJ;
import net.imagej.ops.math.PrimitiveMath;
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
	menuPath = "Plugins>DemonstrateOstravaParadigm")
public class DemonstrateParadigmAdd implements Command {

	private static int step;
	private static int repetitionCount;

	// ImageJServerParadigm-specific stuff
	private static List<String> hosts = new LinkedList<>();

	// LocalMultithreadedParadigm-specific stuff
	private static int maxNumberOfLocalWorkers;

	// HeappeParadigm-specific stuff
	private static int remotePort;
	private static int numberOfNodes;

	public static final Logger log = LoggerFactory.getLogger(
		DemonstrateParadigmAdd.class);

	@Parameter
	private ParallelService parallelService;

	public static void main(final String... args) {

		final Iterator<String> argIter = Arrays.asList(args).iterator();
		argIter.next();
		repetitionCount = Integer.parseInt(argIter.next());
		step = Integer.parseInt(argIter.next());

		if (args[0].equals("-s")) {
			while (argIter.hasNext()) {
				hosts.add(argIter.next());
			}
		}
		else if (args[0].equals("-l")) {
			maxNumberOfLocalWorkers = Integer.parseInt(argIter.next());
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
		ij.command().run(DemonstrateParadigmAdd.class, true);
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

		List<P_Input> inputs = prepareInputs();

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

				doTest(paradigm, inputs, hosts.size());
			}
			else if (numberOfNodes > 0) {
				((HeappeParadigm) paradigm).setPort(remotePort);
				((HeappeParadigm) paradigm).setNumberOfNodes(numberOfNodes);
				((HeappeParadigm) paradigm).init();
				doTest(paradigm, inputs, numberOfNodes);
			}
			else {
				for (int numberOfWorkers =
					maxNumberOfLocalWorkers; 0 < numberOfWorkers; numberOfWorkers--)
				{
					((LocalMultithreadedParadigm) paradigm).setPoolSize(numberOfWorkers);
					paradigm.init();
					doTest(paradigm, inputs, numberOfWorkers);
				}
			}
		}
	}

	private List<P_Input> prepareInputs() {
		final List<P_Input> inputs = new LinkedList<>();
		for (double angle = step; angle < 360; angle += step) {
			inputs.add(new P_Input(PrimitiveMath.DoubleMultiply.class, angle, angle));
		}
	
		return inputs;
	}

	private void doTest(final ParallelizationParadigm paradigm,
		final Collection<P_Input> inputs, final int numberOfWorkers)
	{

		log.info("Number of workers: " + numberOfWorkers);
		for (int i = 0; i < repetitionCount; i++) {
			final long startTime = System.currentTimeMillis();
			
			List<Class<? extends Command>> commands = inputs.stream().map(inp -> inp.getCommand()).collect(Collectors.toList());
			List<Map<String,?>> parameters = inputs.stream().map(inp -> {
				Map<String,Object> par = new HashMap<>();
				par.put("a", inp.getA());
				par.put("b", inp.getB());
				return par;
			}).collect(Collectors.toList());
			List<Map<String,?>> result = paradigm.runAll(commands, parameters);
			log.info("result: " + result);
			
			final long endTime = System.currentTimeMillis();
			final double timeNeededInSec = (endTime - startTime) / 1000.;
			final String resultStr = "Number of workers: " + numberOfWorkers +
				", time: " + timeNeededInSec;
			writeResult(resultStr);
			log.info("Done iteration: " + resultStr);
		}
	}

	

	private String suffix(final Path path) {
		return path.toString().substring(path.toString().lastIndexOf('.'));
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

	private static class P_Input {

		
		private double a;
		private double b;
		private Class<? extends Command> command;

		public P_Input(final Class<? extends Command> command, final double a, final double b) {
			this.a = a;
			this.b = b;
			this.command = command;
		}

		public Class<? extends Command> getCommand() {
			return command;
		}
		
		
		public double getA() {
			return a;
		}
		
		
		public double getB() {
			return b;
		}

		@Override
		public String toString() {
			return "P_Input [a=" + a + ", b=" + b + "]";
		}

	}
}
