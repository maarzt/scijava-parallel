
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
import java.util.stream.Stream;

import cz.it4i.parallel.TestParadigm;
import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.Routines;

public class RotateFile {

	private static final String OUTPUT_DIRECTORY = "output";

	private static String URL_OF_IMAGE_TO_ROTATE =
		"https://upload.wikimedia.org/wikipedia/en/7/7d/Lenna_%28test_image%29.png";

	private final static Logger log = LoggerFactory.getLogger(RotateFile.class);

	private final static int step = 30;

	private static Path imageToRotate;

	public static void main(String[] args) {
		final Context context = new Context();
		try ( ParallelizationParadigm paradigm = TestParadigm.localImageJServer( Config.getFijiExecutable(), context ) ) {
			callRemotePlugin(paradigm);
		}
	}

	private static void callRemotePlugin(final ParallelizationParadigm paradigm) {
		try {
			final Path outputDirectory = prepareOutputDirectory();
			final List< Map< String, Object > > parametersList = initParameters();

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

	private static List< Map< String, Object > > initParameters()
	{
		final List<Map<String, Object>> parametersList = new LinkedList<>();
		Path path = getImageToRotate();
		for (double angle = step; angle < 360; angle += step) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("dataset", path);
			parameters.put("angle", angle);
			parametersList.add(parameters);
		}
		return parametersList;
	}

	private static Path getImageToRotate() {
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

	private static Path getResultPath(Path outputDirectory, Double angle) {
		return outputDirectory.resolve("result_" + angle + getSuffix(imageToRotate
			.getFileName().toString()));
	}

	private static Path prepareOutputDirectory() {
		Path outputDirectory = Paths.get(OUTPUT_DIRECTORY);
		if (!Files.exists(outputDirectory)) {
			Routines.runWithExceptionHandling(() -> Files.createDirectories(
				outputDirectory), log, "create directory");
		}
		return outputDirectory;
	}
}
