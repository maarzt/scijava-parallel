
package test;

import static cz.it4i.parallel.Routines.runWithExceptionHandling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RotateFile extends AbstractBaseDemonstrationExample {

	private final static Logger log = LoggerFactory.getLogger(RotateFile.class);

	@Parameter
	private int step = 30;

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(RotateFile.class, true);
	}

	@Override
	protected void callRemotePlugin(final ParallelizationParadigm paradigm) {
		final Path outputDirectory = prepareOutputDirectory();
		final List<Map<String, Object>> parametersList = new LinkedList<>();
		initParameters(parametersList);

		final List<Map<String, Object>> results = paradigm.runAll(
			RotateImageXY.class, parametersList);
		final Iterator<Map<String, Object>> inputIterator = parametersList
			.iterator();
		for (Map<String, ?> result : results) {
			runWithExceptionHandling(() -> Files.move((Path) result.get("dataset"),
				getResultPath(outputDirectory, (Double) inputIterator.next().get(
					"angle")), StandardCopyOption.REPLACE_EXISTING), log, "moving file");
		}
	}

	protected void initParameters(final List<Map<String, Object>> parameterList) {

		Path path = getImageToRotate();
		for (double angle = step; angle < 360; angle += step) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("dataset", path);
			parameters.put("angle", angle);
			parameterList.add(parameters);
		}
	}

	protected int getStep() {
		return step;
	}
}
