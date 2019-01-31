
package test;

import static cz.it4i.parallel.Routines.getSuffix;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.imagej.ImageJ;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigmProfile;
import org.scijava.plugin.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.ImageJServerParadigm;
import cz.it4i.parallel.Routines;

abstract public class AbstractBaseDemonstrationExample implements Command {

	private static final String OUTPUT_DIRECTORY = "output";

	private List<String> hosts = Arrays.asList("localhost:8080");

	private static String URL_OF_IMAGE_TO_ROTATE =
		"https://upload.wikimedia.org/wikipedia/en/7/7d/Lenna_%28test_image%29.png";

	private static final Logger log = LoggerFactory.getLogger(
		AbstractBaseDemonstrationExample.class);

	@Parameter
	private ParallelService parallelService;

	@Parameter
	private Context context;

	private Path imageToRotate;

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(AbstractBaseDemonstrationExample.class, true);
	}

	@Override
	public void run() {
		try {
			try (AbstractImageJServerRunner imageJServerRunner =
				constructImageJServerRunner())
			{
				imageJServerRunner.startIfNecessary();
				hosts = imageJServerRunner.getPorts().stream().map(
					port -> "localhost:" + port).collect(Collectors.toList());
				try (ParallelizationParadigm paradigm = configureParadigm()) {
					callRemotePlugin(paradigm);
				}

			}
			finally {
				if (imageToRotate != null && Files.exists(imageToRotate)) {
					Routines.runWithExceptionHandling(() -> Files.delete(imageToRotate),
						log, "delete rotated image");
				}
			}
		}
		catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
		finally {
			System.exit(0);
		}
	}

	protected AbstractImageJServerRunner constructImageJServerRunner() {
		return new ImageJServerRunner();
	}

	abstract protected void callRemotePlugin(
		final ParallelizationParadigm paradigm);

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

	private ParallelizationParadigm configureParadigm() {
		parallelService.deleteProfiles();
		parallelService.addProfile(new ParallelizationParadigmProfile(
			ImageJServerParadigm.class, "lonelyBiologist01"));
		parallelService.selectProfile("lonelyBiologist01");

		ParallelizationParadigm paradigm = parallelService.getParadigm();
		((ImageJServerParadigm) paradigm).setHosts(hosts);
		paradigm.init();
		return paradigm;
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
}
