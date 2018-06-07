package test.learnathon;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.IntStream;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.ImageJServerParadigm;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ParadigmDemonstration")
public class Demonstration implements Command {

	private static int step = 10;

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

		// prepare inputs
		Collection<Integer> angles = new LinkedList<>();
		Path fileToRotate = Paths.get("/tmp/input/lena.jpg");
		for (int angle = step; angle < 360; angle += step) {
			angles.add(angle);
		}

		ParallelizationParadigm paradigm = parallelService.getParadigm(ImageJServerParadigm.class);
		Collection<String> hosts = new LinkedList<>();
		hosts.add("localhost:10001");
		hosts.add("localhost:10002");
		hosts.add("localhost:10003");
		hosts.add("localhost:10004");
		((ImageJServerParadigm) paradigm).setHosts(hosts);
		
		// common initialization
		paradigm.init();

		paradigm.parallelFor(angles, (angle, executionContext) -> {
			log.info("processing angle=" + angle);
			IntStream.iterate(0, n->n+step).takeWhile(n->n<10);
			Dataset ds = executionContext.importData(fileToRotate);
			RotateImageXY<?> command = executionContext.getRemoteCommand(RotateImageXY.class);
			command.setAngle(angle);
			command.setDataset(ds);
			command.run();
			ds = command.getDataset();
			executionContext.exportData(ds, Paths.get("/tmp/output/result_" + angle + ".jpg"));
			log.info("DONE: processing angle=" + angle);
		});
	}
}
