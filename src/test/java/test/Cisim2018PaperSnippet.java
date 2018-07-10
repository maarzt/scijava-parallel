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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>Cisim2018Demonstration")
public class Cisim2018PaperSnippet implements Command {

	private static int step = 10;

	private static List<String> hosts;
	public static final Logger log = LoggerFactory.getLogger(test.Cisim2018PaperSnippet.class);

	@Parameter
	ParallelService parallelService;

	@Override
	public void run() {
		final Collection<P_Input> inputs = prepareInputs();
		final Map<P_Input, Path> outputs = new HashMap<>();
		final ParallelizationParadigm paradigm = parallelService.getParadigm(ImageJServerParadigm.class);
		paradigm.parallelFor(inputs, (input, task) -> {
			Dataset ds = task.importData(input.dataset);
			final RotateImageXY<?> command = task.getRemoteCommand(RotateImageXY.class);
			command.setAngle(input.angle);
			command.setDataset(ds);
			command.run();
			ds = command.getDataset();
			final Path outputPath = constructOutputPath(input);
			task.exportData(ds, outputPath);
			outputs.put(input, outputPath);
		});
	}

	private Path constructOutputPath(final P_Input input) {
		return Paths.get(getOutputFilesPattern() + input.angle + suffix(input.dataset));
	}

	private String suffix(final Path path) {
		return path.toString().substring(path.toString().lastIndexOf('.'));
	}

	private Collection<P_Input> prepareInputs() {
		final Collection<P_Input> inputs = new LinkedList<>();
		Path file;
		try {
			file = Files
					.newDirectoryStream(Paths.get(getInputDirectory()),
							p -> p.toString().endsWith(PNG_SUFFIX) || p.toString().endsWith(JPG_SUFFIX))
					.iterator().next();
			for (int angle = step; angle < 360; angle += step) {
				inputs.add(new P_Input(file, angle));
			}
		} catch (final IOException e) {
			log.error(e.getMessage(), e);
		}
		return inputs;
	}

	public static void main(final String... args) {
		Cisim2018PaperSnippet.hosts = new LinkedList<>();
		if (!args[0].equals("-l")) {
			final Iterator<String> argIter = Arrays.asList(args).iterator();

			step = Integer.parseInt(argIter.next());
			while (argIter.hasNext()) {
				Cisim2018PaperSnippet.hosts.add(argIter.next());
			}
		} else {
			final Iterator<String> argIter = Arrays.asList(args).iterator();
			argIter.next();

			step = Integer.parseInt(argIter.next());

		}
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.command().run(Cisim2018PaperSnippet.class, true);
	}

	private static class P_Input {
		Path dataset;
		double angle;

		public P_Input(final Path dataset, final double angle) {
			this.dataset = dataset;
			this.angle = angle;
		}

		@Override
		public String toString() {
			return "P_Input [dataset=" + dataset + ", angle=" + angle + "]";
		}

	}

}
