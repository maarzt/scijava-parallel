
package test;

import static cz.it4i.parallel.Routines.runWithExceptionHandling;

import com.google.common.collect.Streams;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.TestParadigm;

public class RotateFileAsync {

	private final static Logger log = LoggerFactory.getLogger(
		RotateFileAsync.class);

	public static void main(String[] args) {
		Context context = new Context();
		try ( ParallelizationParadigm paradigm = TestParadigm.localImageJServer( Config.getFijiExecutable(), context ) )
		{
			List< Map< String, Object > > parametersList = RotateFile.initParameters();
			List< CompletableFuture< Map< String, Object > > > results = paradigm.runAllAsync(
					RotateImageXY.class, parametersList );
			asyncSaveOutputs( parametersList, results );
		}
	}

	private static void asyncSaveOutputs( List< Map< String, Object > > parametersList, List< CompletableFuture< Map< String, Object > > > results )
	{
		// @formatter:off
		Path outputDirectory = RotateFile.prepareOutputDirectory();
		Streams.zip(results.stream(), parametersList.stream().map(
			inputParams -> (Double) inputParams.get("angle")),
			(future, angle) -> future.thenAccept(
				result -> {
					Path src = (Path) result.get("dataset");
					Path dst = outputDirectory.resolve("result_" + angle + ".tif");
					runWithExceptionHandling(() -> Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING));
					log.info("moved: " + src + " -> " + dst);
					}))
		.forEach(future -> waitForFuture(future));
		// @formatter:on
	}

	private static void waitForFuture( CompletableFuture< Void > future ) {
		try {
			future.get();
		}
		catch (InterruptedException | ExecutionException exc) {
			log.error("wait for completition", exc);
		}
	}
}
