package cz.it4i.parallel;

import java.util.List;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImageJ;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>ParallelServiceTestSuite")
public class TestSuite implements Command {
	
	@Parameter
	ParallelService parallelService;
	
	@Override
	public void run() {
		
		List<ParallelizationParadigm> instances = parallelService.getInstances();

	}
	
	public static void main(final String... args) {
		
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.launch(args);
		ij.command().run(TestSuite.class, true);
	}

}
