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
import java.util.Collection;
import java.util.LinkedList;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.HeappeParadigm;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>DemonstrateOstravaParadigm")
public class DemonstrateHeAppeParadigm implements Command {

	private static int step = 10;
	private static int port = 8080;

	private static int numberOfHosts;
	public static final Logger log = LoggerFactory.getLogger(DemonstrateHeAppeParadigm.class);

	@Parameter
	ParallelService parallelService;

	@Override
	public void run() {
		Collection<P_Input> inputs = prepareInputs();
		try (HeappeParadigm remoteParadigm = parallelService.getParadigm(HeappeParadigm.class)) {
			remoteParadigm.setPort(port);
			remoteParadigm.init();
			doTest(remoteParadigm, inputs, numberOfHosts);
		}			

	}

	private Collection<P_Input> prepareInputs() {
		Collection<P_Input> inputs = new LinkedList<>();
		Path file;
		try {
			file = Files
					.newDirectoryStream(Paths.get(getInputDirectory()),
							p -> p.toString().endsWith(PNG_SUFFIX) || p.toString().endsWith(JPG_SUFFIX))
					.iterator().next();
			for (int angle = step; angle < 360; angle += step) {
				inputs.add(new P_Input(file, String.valueOf(angle)));
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return inputs;
	}

	private void doTest(ParallelizationParadigm paradigm, Collection<P_Input> inputs, int numberOfWorkers) {
		log.info("Number of workers: " + numberOfWorkers);
		long time = System.currentTimeMillis();
		paradigm.parallelFor(inputs, (input, task) -> {
			// log.info("processing angle=" + input.angle);
			Dataset ds = task.importData(input.dataset);
			RotateImageXY<?> command = task.getRemoteCommand(RotateImageXY.class);
			command.setAngle(Double.parseDouble(input.angle));
			command.setDataset(ds);
			command.run();
			ds = command.getDataset();
			task.exportData(ds, constructOutputPath(input));
		});
		long time2 = System.currentTimeMillis();
		double sec = (time2 - time) / 1000.;
		String resultStr = "Number of workers: " + numberOfWorkers + ", time: " + sec;
		writeResult(resultStr);
		log.info("done iteration: " + resultStr);
}

	private Path constructOutputPath(P_Input input) {
		return Paths.get(getOutputFilesPattern() + input.angle + suffix(input.dataset));
	}

	private String suffix(Path path) {
		return path.toString().substring(path.toString().lastIndexOf('.'));
	}

	private void writeResult(String resultStr) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(getResultFile()), StandardOpenOption.APPEND,
				StandardOpenOption.CREATE)) {
			bw.write(resultStr + "\n");
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void main(final String... args) {

		DemonstrateHeAppeParadigm.numberOfHosts = Integer.parseInt(args[0]);
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrateHeAppeParadigm.class, true);
	}

	private static class P_Input {
		Path dataset;
		String angle;

		public P_Input(Path dataset, String angle) {
			this.dataset = dataset;
			this.angle = angle;
		}

		@Override
		public String toString() {
			return "P_Input [dataset=" + dataset + ", angle=" + angle + "]";
		}

	}

}
