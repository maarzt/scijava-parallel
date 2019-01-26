
package test;

import static cz.it4i.parallel.Routines.runWithExceptionHandling;

import com.google.common.collect.Streams;

import java.nio.file.Files;
import java.nio.file.Path;
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

@Plugin(type = Command.class, headless = true)
public class DemonstrationExampleAsync extends DemonstrationExample {

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrationExampleAsync.class, true);
	}

	@Override
	protected void doRotation(ParallelizationParadigm paradigm) {
		Path outputDirectory = prepareOutputDirectory();

		List<Map<String, Object>> parametersList = new LinkedList<>();
		initParameters(parametersList);

		List<CompletableFuture<Map<String, Object>>> results = paradigm.runAllAsync(
			RotateImageXY.class, parametersList);

		Streams.zip(results.stream(), parametersList.stream().map(
			inputParams -> (Double) inputParams.get("angle")), //
			(future, angle) -> future.thenAccept(result -> runWithExceptionHandling(
				() -> Files.move((Path) result.get("dataset"), getResultPath(
					outputDirectory, angle)), log, "moving file"))) //
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
}
