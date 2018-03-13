package cz.it4i.parallel;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.IRotateImageXY;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>ParallelServiceTestSuite")
public class TestSuite2 implements Command {

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.TestSuite2.class);

	@Parameter
	ParallelService parallelService;

	@Override
	public void run() {

		ParallelizationParadigm paradigm = parallelService.getParadigms().get(0);

		class P_Input {
			Path dataset;
			String angle;

			public P_Input(Path dataset, String angle) {
				this.dataset = dataset;
				this.angle = angle;
			}
		}
		Collection<P_Input> inputs = new LinkedList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("/tmp/input/"))) {
			int angle = 10;
			for (Path file : ds) {
				inputs.add(new P_Input(file, String.valueOf(angle)));
				angle += 10;
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		
		
		paradigm.parallelLoop(inputs, (input, task)->{
			
			IRotateImageXY command = task.getRemoteModule(IRotateImageXY.class);
			command.setAngle(input.angle);
			command.setDataset(input.dataset);
			command.run();
			Path result = command.getDataset();
			log.info("result is " + result);
			
		});
	}

	public static void main(final String... args) {

		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.launch(args);
		ij.command().run(TestSuite2.class, true);
	}

}
