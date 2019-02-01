
package test;

import static cz.it4i.parallel.Routines.getSuffix;
import static cz.it4i.parallel.Routines.runWithExceptionHandling;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import cz.it4i.parallel.Routines;

public class RotateFile extends AbstractBaseDemonstrationExample {

	private static final String OUTPUT_DIRECTORY = "output";

	private static String URL_OF_IMAGE_TO_ROTATE =
		"https://upload.wikimedia.org/wikipedia/en/7/7d/Lenna_%28test_image%29.png";

	private final static Logger log = LoggerFactory.getLogger(RotateFile.class);

	@Parameter
	private int step = 30;

	private Path imageToRotate;

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(RotateFile.class, true);
	}

	@Override
	protected void callRemotePlugin(final ParallelizationParadigm paradigm) {
		try {
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
						"angle")), StandardCopyOption.REPLACE_EXISTING), log,
					"moving file");
			}
		}
		finally {
			if (imageToRotate != null && Files.exists(imageToRotate)) {
				Routines.runWithExceptionHandling(() -> Files.delete(imageToRotate),
					log, "delete rotated image");
			}
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

	final protected Path getImageToRotate() {
		if (imageToRotate == null) {
			try (InputStream is = new URL(URL_OF_IMAGE_TO_ROTATE).openStream()) {
				imageToRotate = Files.createTempFile("", URL_OF_IMAGE_TO_ROTATE
					.substring(URL_OF_IMAGE_TO_ROTATE.lastIndexOf('.')));
				Files.copy(is, imageToRotate, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException exc) {
				log.error("download image", exc);
				throw new RuntimeException(exc);
			}
		}
		return imageToRotate;
	}

	final protected Path getResultPath(Path outputDirectory, Double angle) {
		return outputDirectory.resolve("result_" + angle + getSuffix(imageToRotate
			.getFileName().toString()));
	}

	final protected Path prepareOutputDirectory() {
		Path outputDirectory = Paths.get(OUTPUT_DIRECTORY);
		if (!Files.exists(outputDirectory)) {
			Routines.runWithExceptionHandling(() -> Files.createDirectories(
				outputDirectory), log, "create directory");
		}
		return outputDirectory;
	}
}
