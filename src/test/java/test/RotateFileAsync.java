
package test;

import static cz.it4i.parallel.Routines.runWithExceptionHandling;

import com.google.common.collect.Streams;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = Command.class, headless = true)
public class RotateFileAsync extends RotateFile {

	private final static Logger log = LoggerFactory.getLogger(
		RotateFileAsync.class);

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(RotateFileAsync.class, true);
	}

	@Override
	protected void callRemotePlugin(ParallelizationParadigm paradigm) {
		Path outputDirectory = prepareOutputDirectory();

		List<Map<String, Object>> parametersList = new LinkedList<>();
		initParameters(parametersList);

		List<CompletableFuture<Map<String, Object>>> results = paradigm.runAllAsync(
			RotateImageXY.class, parametersList);
	// @formatter:off
		Streams.zip(results.stream(), parametersList.stream().map(
			inputParams -> (Double) inputParams.get("angle")), 
			(future, angle) -> future.thenAccept(
				result -> {
					Path src = (Path) result.get("dataset");
					Path dst = getResultPath(outputDirectory, angle);
					runWithExceptionHandling(() -> Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING), log, "moving file");
					log.info("moved: " + src + " -> " + dst);
					}))
		.forEach(future -> waitForFuture(future));
	// @formatter:on
	}

	private void waitForFuture(CompletableFuture<Void> future) {
		try {
			future.get();
		}
		catch (InterruptedException | ExecutionException exc) {
			log.error("wait for completition", exc);
		}
	}
}
