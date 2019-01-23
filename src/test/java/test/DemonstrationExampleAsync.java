package test;

import com.google.common.collect.Streams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
public class DemonstrationExampleAsync implements Command {

	private static final String OUTPUT_DIRECTORY = "output";

	private static int step = 30;

	private static List<String> hosts = Arrays.asList("localhost:8080");
	
	private static String URL_OF_IMAGE_TO_ROTATE = "https://upload.wikimedia.org/wikipedia/en/7/7d/Lenna_%28test_image%29.png";

	private static final String MEODULES_URL = "http://localhost:8080/modules";
	
	private static String[] IMAGEJ_SERVER_WITH_PARAMETERS = { "ImageJ-linux64",
		"-Dimagej.legacy.modernOnlyCommands=true", "--", "--ij2", "--headless",
		"--server" };

	public static final Logger log = LoggerFactory.getLogger(
		DemonstrationExampleAsync.class);

	@Parameter
	private ParallelService parallelService;

	private Process imageJServerProcess;

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrationExampleAsync.class, true);
	}

	@Override
	public void run() {
		startImageJServerIfNecessary();
		
		try( ParallelizationParadigm paradigm = configureParadigm() ) {
			doRotation(paradigm);
		}
		if (imageJServerProcess != null) {
			imageJServerProcess.destroy();
		}
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

	private void initParameters(ParallelizationParadigm paradigm ,List<Class<? extends Command>> commands, List<Map<String, ?>> parametersList) {
		
		Path path = getImagetToRotate();
				
		for (double angle = step; angle < 360; angle += step) {
			commands.add(RotateImageXY.class);
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("dataset", paradigm.createRemoteDataset(path.toUri()));
			parameters.put("angle", angle);
			parametersList.add(parameters);
		}
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

	private void doRotation(final ParallelizationParadigm paradigm)
	{
		Path outputDirectory = prepareoutputDirectory();
		
		List<Map<String,?>> parametersList = new LinkedList<>();
		List<Class<? extends Command>> commands = new LinkedList<>();
		initParameters(paradigm,commands,parametersList);
		
		List<CompletableFuture<Map<String, ?>>> results = paradigm.runAllAsync(commands, parametersList);
		
		Streams.zip(results.stream(), parametersList.stream().map(inputParams ->(Double) inputParams.get("angle")), // 
			(future, angle) -> future.thenAccept(result -> paradigm.exportWriteableDataset( //
					                                               (WriteableDataset) result.get("dataset") //
					                                               , getResultURI(outputDirectory,angle)))) //
		.forEach(future -> waitForFuture(future));
	
	}

	private void waitForFuture(CompletableFuture<Void> future) {
		try {
			future.get();
		}
		catch (InterruptedException | ExecutionException exc) {
			log.error("wait for completition", exc);
		}
	}

	private URI getResultURI(Path outputDirectory,Double angle) {
		return outputDirectory.resolve("result_" + angle + ".jpg").toUri();
	}

	private void startImageJServerIfNecessary() {
		if (!checkImageJServerRunning()) {
			startImageJServer();
		}
		
	}

	private void startImageJServer() {
		boolean running = false;
		String[] command = IMAGEJ_SERVER_WITH_PARAMETERS.clone();
		command[0] = Paths.get(Config.getFijiLocation(), command[0]).toString();
		try {
			ProcessBuilder pb = new ProcessBuilder(command).inheritIO();
			imageJServerProcess =  pb.start();
			do {
				try {
					if(checkModulesURL() == 200) {
						running = true;
					}
				} catch (IOException e) {
					//ignore waiting for start
				}
			} while(!running);
		}
		catch (IOException exc) {
			log.error("start imageJServer", exc);
			throw new RuntimeException(exc);
		}
	}

	private boolean checkImageJServerRunning() {
		boolean running = true;
		try {
			if ( checkModulesURL() != 200) {
				throw new IllegalStateException("Different server than ImageJServer is running on localhost:8080");
			}
		}
		catch (ConnectException exc) {
			running = false;
		}
		catch (IOException exc) {
			log.error("connect ot ImageJServer", exc);
			throw new RuntimeException(exc);
		}
		return running;
	}

	private int checkModulesURL() throws IOException, MalformedURLException,
		ProtocolException
	{
		HttpURLConnection hc;
		hc = (HttpURLConnection) new URL(MEODULES_URL).openConnection();
		hc.setRequestMethod("GET");
		hc.connect();
		hc.disconnect();
		return hc.getResponseCode();
	}

	private Path prepareoutputDirectory() {
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
}