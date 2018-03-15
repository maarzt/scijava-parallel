package cz.it4i.parallel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.IRotateImageXY;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>ParallelServiceTestSuite")
public class TestSuite2 implements Command {

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.TestSuite2.class);

	@Parameter
	ParallelService parallelService;

	@Override
	public void run() {

		ParallelizationParadigm paradigm = parallelService.getParadigms().get(0);
		long time = System.currentTimeMillis();
		class P_Input {
			Path dataset;
			String angle;

			public P_Input(Path dataset, String angle) {
				this.dataset = dataset;
				this.angle = angle;
			}
		}
		Collection<P_Input> inputs = new LinkedList<>();
		Path file;
		try {
			file = Files.newDirectoryStream(Paths.get("/tmp/input/"), p->p.toString().endsWith(".png")).iterator().next();
			for(int angle = 1; angle < 360; angle++) {
				inputs.add(new P_Input(file, String.valueOf(angle)));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		long time2 = System.currentTimeMillis();
		double sec = (time2 - time)/1000.;
		log.info("Duration: " + sec + " s");
	}

	public static void main(final String... args) {

		List<String> hosts = new LinkedList<>();
		for(String arg: args) {
			hosts.add(arg);
		}
		SimpleOstravaParadigm.addHosts(hosts);
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.launch(args);
		ij.command().run(TestSuite2.class, true);
		System.exit(0);
	}

}
