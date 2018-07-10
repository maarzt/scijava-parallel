
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.ImageJServerParadigm;
import cz.it4i.parallel.LocalMultithreadedParadigm;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>DemonstrateOstravaParadigm")
public class DemonstrateParadigm implements Command {

	private static int repetitionCount = 10;
	private static int step = 10;
	private static int port = 8080;

	private static List<String> hosts;
	private static int maxNumberOfLocalWorkers;
	public static final Logger log = LoggerFactory.getLogger(
		DemonstrateParadigm.class);

	@Parameter
	ParallelService parallelService;

	@Override
	public void run() {

		final Collection<P_Input> inputs = prepareInputs();

		if (hosts.size() > 0) {
			try (ImageJServerParadigm remoteParadigm = parallelService.getParadigm(
				ImageJServerParadigm.class))
			{
				remoteParadigm.setHosts(hosts);
				remoteParadigm.setPort(port);
				remoteParadigm.init();
				doTest(remoteParadigm, inputs, hosts.size());
			}
		}
		else {
			try (LocalMultithreadedParadigm localParadigm = parallelService
				.getParadigm(LocalMultithreadedParadigm.class))
			{
				for (int numberOfWorkers =
					maxNumberOfLocalWorkers; 0 < numberOfWorkers; numberOfWorkers--)
				{
					localParadigm.setPoolSize(numberOfWorkers);
					localParadigm.init();
					doTest(localParadigm, inputs, numberOfWorkers);
				}
			}
		}
	}

	private Collection<P_Input> prepareInputs() {
		final Collection<P_Input> inputs = new LinkedList<>();
		Path file;
		try {
			file = Files.newDirectoryStream(Paths.get(getInputDirectory()), p -> p
				.toString().endsWith(PNG_SUFFIX) || p.toString().endsWith(JPG_SUFFIX))
				.iterator().next();
			for (int angle = step; angle < 360; angle += step) {
				inputs.add(new P_Input(file, String.valueOf(angle)));
			}
		}
		catch (final IOException e) {
			log.error(e.getMessage(), e);
		}
		return inputs;
	}

	private void doTest(final ParallelizationParadigm paradigm,
		final Collection<P_Input> inputs, final int numberOfWorkers)
	{
		log.info("Number of workers: " + numberOfWorkers);
		for (int i = 0; i < repetitionCount; i++) {
			final long time = System.currentTimeMillis();
			paradigm.parallelFor(inputs, (input, task) -> {
				// log.info("processing angle=" + input.angle);
				Dataset ds = task.importData(input.dataset);
				final RotateImageXY<?> command = task.getRemoteCommand(
					RotateImageXY.class);
				command.setAngle(Double.parseDouble(input.angle));
				command.setDataset(ds);
				command.run();
				ds = command.getDataset();
				task.exportData(ds, constructOutputPath(input));
			});
			final long time2 = System.currentTimeMillis();
			final double sec = (time2 - time) / 1000.;
			final String resultStr = "Number of workers: " + numberOfWorkers +
				", time: " + sec;
			writeResult(resultStr);
			log.info("done iteration: " + resultStr);
		}
	}

	private Path constructOutputPath(final P_Input input) {
		return Paths.get(getOutputFilesPattern() + input.angle + suffix(
			input.dataset));
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

	public static void main(final String... args) {

		DemonstrateParadigm.hosts = new LinkedList<>();
		if (!args[0].equals("-l")) {
			final Iterator<String> argIter = Arrays.asList(args).iterator();

			repetitionCount = Integer.parseInt(argIter.next());
			step = Integer.parseInt(argIter.next());
			while (argIter.hasNext()) {
				DemonstrateParadigm.hosts.add(argIter.next());
			}
		}
		else {
			final Iterator<String> argIter = Arrays.asList(args).iterator();
			argIter.next();
			repetitionCount = Integer.parseInt(argIter.next());
			step = Integer.parseInt(argIter.next());
			maxNumberOfLocalWorkers = Integer.parseInt(argIter.next());
		}
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrateParadigm.class, true);
	}

	private static class P_Input {

		Path dataset;
		String angle;

		public P_Input(final Path dataset, final String angle) {
			this.dataset = dataset;
			this.angle = angle;
		}

		@Override
		public String toString() {
			return "P_Input [dataset=" + dataset + ", angle=" + angle + "]";
		}

	}

}
