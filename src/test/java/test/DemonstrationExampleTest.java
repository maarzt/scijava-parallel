
package test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imagej.Extents;
import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.io.IOService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = Command.class, headless = true)
public class DemonstrationExampleTest extends DemonstrationExample {

	private final static Logger log = LoggerFactory.getLogger(
		test.DemonstrationExampleTest.class);

	@Parameter
	private IOService ioService;

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrationExampleTest.class, true);
	}

	@Override
	protected void doRotation(ParallelizationParadigm paradigm) {
		List<Map<String, Object>> parametersList = new LinkedList<>();
		initParameters(parametersList);

		List<Map<String, Object>> results = paradigm.runAll(
			"net.imagej.server.TestingCommand", parametersList);
		log.info("result: " + results);
	}

	@Override
	protected void initParameters(List<Map<String, Object>> parametersList) {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("a", 11);
		parameters.put("interval", new Extents(new long[] { 10 }, new long[] {
			10 }));
		parametersList.add(parameters);
	}
}
