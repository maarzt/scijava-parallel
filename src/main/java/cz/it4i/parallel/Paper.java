package cz.it4i.parallel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ParallelServiceTestSuite")
public class Paper implements Command {

	private static int step = 10;
	
	private static List<String> hosts;
	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.Paper.class);

	@Parameter
	ParallelService parallelService;

	@Override
	public void run() {
		Collection<P_Input> inputs = prepareInputs();
		Collection<Path> outputs = new LinkedList<>();
		ParallelizationParadigm paradigm = parallelService
				.getParadigms().get(0);	
		paradigm.parallelLoop(inputs, (input, task) -> {
			Dataset ds = task.importData(input.dataset);
			RotateImageXY<?> command = task.getRemoteModule(RotateImageXY.class);
			command.setAngle(input.angle);
			command.setDataset(ds);
			command.run();
			ds = command.getDataset();
			Path result = task.exportData(ds);
			outputs.add(result);
		});
	}

	private Collection<P_Input> prepareInputs() {
		Collection<P_Input> inputs = new LinkedList<>();
		Path file;
		try {
			file = Files.newDirectoryStream(Paths.get("/tmp/input/"), p -> p.toString().endsWith(".png") || p.toString().endsWith(".jpg")).iterator()
					.next();
			for (int angle = step; angle < 360; angle += step) {
				inputs.add(new P_Input(file, angle));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage(), e);
		}
		return inputs;
	}

	
	public static void main(final String... args) {

		Paper.hosts = new LinkedList<>();
		if (!args[0].equals("-l")) {
			Iterator<String> argIter = Arrays.asList(args).iterator();
			
			
			step = Integer.parseInt(argIter.next());
			while(argIter.hasNext()) {
				Paper.hosts.add(argIter.next());
			}
		} else {
			Iterator<String> argIter = Arrays.asList(args).iterator();
			argIter.next();
			
			step = Integer.parseInt(argIter.next());
			
		}
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.command().run(Paper.class, true);
	}

	private static class P_Input {
		Path dataset;
		double angle;

		public P_Input(Path dataset, double angle) {
			this.dataset = dataset;
			this.angle = angle;
		}

		@Override
		public String toString() {
			return "P_Input [dataset=" + dataset + ", angle=" + angle + "]";
		}

	}

}
