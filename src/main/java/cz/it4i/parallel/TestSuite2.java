package cz.it4i.parallel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

	private static int count = 10;
	private static int minThreads = 1;
	private static int maxThreads = 48;
	private static final int minHosts = 1;

	private static List<String> hosts;
	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.TestSuite2.class);

	@Parameter
	ParallelService parallelService;

	@Override
	public void run() {

		ParallelizationParadigm paradigm = parallelService.getParadigms().get(0);
		ImageJServerParadigm sop = (ImageJServerParadigm) paradigm;
		Collection<P_Input> inputs = new LinkedList<>();
		Path file;
		try {
			file = Files.newDirectoryStream(Paths.get("/tmp/input/"), p -> p.toString().endsWith(".png")).iterator()
					.next();
			for (int angle = 1; angle < 360; angle++) {
				inputs.add(new P_Input(file, String.valueOf(angle)));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage(), e);
		}
		for (int numberOfHosts = hosts.size(); minHosts <=numberOfHosts; numberOfHosts--) {
			List<String> usedHosts = hosts.subList(0, numberOfHosts);
			sop.setHosts(usedHosts);
			for (int numberOfThreads = minThreads; numberOfThreads <= maxThreads; numberOfThreads++) {
				sop.setPoolSize(numberOfThreads);
				sop.init();
				doTest(paradigm, inputs, usedHosts.size(), numberOfThreads);
			}
		}
	}

	private void doTest(ParallelizationParadigm paradigm, Collection<P_Input> inputs, int nummberOfWorkers,
			int numberOfThreads) {
		List<Double> resultTimes = new LinkedList<>();
		for (int i = 0; i < count; i++) {
			long time = System.currentTimeMillis();
			paradigm.parallelLoop(inputs, (input, task) -> {

				IRotateImageXY command = task.getRemoteModule(IRotateImageXY.class);
				command.setAngle(input.angle);
				command.setDataset(input.dataset);
				command.run();
				command.getDataset();
				// log.info("result is " + result);

			});
			long time2 = System.currentTimeMillis();
			double sec = (time2 - time) / 1000.;
			resultTimes.add(sec);
		}
		DoubleSummaryStatistics dss = resultTimes.stream().collect(Collectors.summarizingDouble(val -> val));
		log.info("Number of workers =  " + nummberOfWorkers + ", numberOfThreads " + numberOfThreads + ", count: "
				+ dss.getCount() + ", avg:" + dss.getAverage() + ", min:" + dss.getMin() + ", max:" + dss.getMax());
	}

	public static void main(final String... args) {

		TestSuite2.hosts = new LinkedList<>();
		Iterator<String> argIter = Arrays.asList(args).iterator();
		count = Integer.parseInt(argIter.next());
		minThreads = Integer.parseInt(argIter.next());
		maxThreads = Integer.parseInt(argIter.next());
		while(argIter.hasNext()) {
			TestSuite2.hosts.add(argIter.next());
		}
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.launch();
		ij.command().run(TestSuite2.class, true);
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
