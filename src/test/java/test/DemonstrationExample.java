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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigmProfile;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.ImageJServerParadigm;
import cz.it4i.parallel.Routines;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>DemonstrateOstravaParadigm")
public class DemonstrationExample implements Command {

	private static final String OUTPUT_DIRECTORY = "output";

	protected static int step = 30;

	private static List<String> hosts = Arrays.asList("localhost:8080");
	
	private static String URL_OF_IMAGE_TO_ROTATE = "https://upload.wikimedia.org/wikipedia/en/7/7d/Lenna_%28test_image%29.png";
	
	public static final Logger log = LoggerFactory.getLogger(
		DemonstrationExample.class);

	@Parameter
	private ParallelService parallelService;

	private Path imageToRotate;

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrationExample.class, true);
	}

	@Override
	public void run() {
		try(ImageJServerRunner imageJServerRunner = new ImageJServerRunner()) {
			try( ParallelizationParadigm paradigm = configureParadigm() ) {
				doRotation(paradigm);
			}
			
		} finally {
			if (imageToRotate != null && Files.exists(imageToRotate)) {
				Routines.runWithExceptionHandling(()->Files.delete(imageToRotate), log, "delete rotated image");
			}
		}
	}

	protected void doRotation(final ParallelizationParadigm paradigm)
	{
		Path outputDirectory = prepareoutputDirectory();
		List<Map<String, Object>> parametersList = new LinkedList<>();
		List<Class<? extends Command>> commands = new LinkedList<>();
		initParameters(commands,parametersList);
		
		List<Map<String, Object>> results = paradigm.runAll(commands, parametersList);
		Iterator<Map<String,Object>> inputIterator = parametersList.iterator();
		for(Map<String,?> result: results) {
			runWithExceptionHandling(
				() -> Files.move((Path) result.get("dataset")
												, getResultPath(outputDirectory,(Double) inputIterator.next().get("angle"))
												, StandardCopyOption.REPLACE_EXISTING)
				,log, "moving file");
		}
	}

	protected void initParameters(List<Class<? extends Command>> commands, List<Map<String, Object>> parametersList) {
		
		Path path = getImagetToRotate();
		for (double angle = step; angle < 360; angle += step) {
			commands.add(RotateImageXY.class);
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("dataset", path);
			parameters.put("angle", angle);
			parametersList.add(parameters);
		}
	}

	final protected Path getResultPath(Path outputDirectory,Double angle) {
		return outputDirectory.resolve("result_" + angle + getSuffix(imageToRotate.getFileName().toString()));
	}

	final protected Path prepareoutputDirectory() {
		Path outputDirectory = Paths.get(OUTPUT_DIRECTORY);
		if (!Files.exists(outputDirectory)) {
			Routines.runWithExceptionHandling(
				() -> Files.createDirectories(outputDirectory)
				, log, "create directory");
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

	final protected Path getImagetToRotate() {
		if (imageToRotate == null) {
			try(InputStream is = new URL(URL_OF_IMAGE_TO_ROTATE).openStream()) {
				imageToRotate = Files.createTempFile("", URL_OF_IMAGE_TO_ROTATE.substring(URL_OF_IMAGE_TO_ROTATE.lastIndexOf('.')));
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