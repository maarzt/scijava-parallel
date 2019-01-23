package test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
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
import org.scijava.parallel.WriteableDataset;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.ImageJServerParadigm;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>DemonstrateOstravaParadigm")
public class DemonstrationExample implements Command {

	private static final String OUTPUT_DIRECTORY = "output";

	private static int step = 30;

	private static List<String> hosts = Arrays.asList("localhost:8080");
	
	private static String URL_OF_IMAGE_TO_ROTATE = "https://upload.wikimedia.org/wikipedia/en/7/7d/Lenna_%28test_image%29.png";
	
	public static final Logger log = LoggerFactory.getLogger(
		DemonstrationExample.class);

	@Parameter
	private ParallelService parallelService;

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
			
		}
	}

	protected void doRotation(final ParallelizationParadigm paradigm)
	{
		Path outputDirectory = prepareoutputDirectory();
		List<Map<String,?>> parametersList = new LinkedList<>();
		List<Class<? extends Command>> commands = new LinkedList<>();
		initParameters(paradigm,commands,parametersList);
		
		List<Map<String, ?>> results = paradigm.runAll(commands, parametersList);
		Iterator<Map<String,?>> inputIterator = parametersList.iterator();
		for(Map<String,?> result: results) {
			paradigm.exportWriteableDataset((WriteableDataset) result.get("dataset"), getResultURI(outputDirectory,(Double) inputIterator.next().get("angle")));
		}
	}

	final protected void initParameters(ParallelizationParadigm paradigm ,List<Class<? extends Command>> commands, List<Map<String, ?>> parametersList) {
		
		Path path = getImagetToRotate();
				
		for (double angle = step; angle < 360; angle += step) {
			commands.add(RotateImageXY.class);
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("dataset", paradigm.createRemoteDataset(path.toUri()));
			parameters.put("angle", angle);
			parametersList.add(parameters);
		}
	}

	final protected URI getResultURI(Path outputDirectory,Double angle) {
		return outputDirectory.resolve("result_" + angle + ".jpg").toUri();
	}

	final protected Path prepareoutputDirectory() {
		Path outputDirectory = Paths.get(OUTPUT_DIRECTORY);
		if (Files.exists(outputDirectory)) {
			try {
				Files.walk(outputDirectory)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
			}
			catch (IOException exc) {
				log.error("delete directory: " + outputDirectory.toAbsolutePath());
				throw new RuntimeException(exc);
			}
		}
		try {
			Files.createDirectories(outputDirectory);
		}
		catch (IOException exc) {
			log.error("create directory: " + outputDirectory.toAbsolutePath());
			throw new RuntimeException(exc);
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

	private Path getImagetToRotate() {
		try(InputStream is = new URL(URL_OF_IMAGE_TO_ROTATE).openStream()) {
			Path result = Files.createTempFile("", URL_OF_IMAGE_TO_ROTATE.substring(URL_OF_IMAGE_TO_ROTATE.lastIndexOf('.')));
			Files.copy(is, result, StandardCopyOption.REPLACE_EXISTING);
			return result;
		}
		catch (IOException exc) {
			log.error("download image", exc);
			throw new RuntimeException(exc);
		}
	}
}