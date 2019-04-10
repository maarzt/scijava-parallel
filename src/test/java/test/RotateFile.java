
package test;

import static cz.it4i.parallel.Routines.runWithExceptionHandling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

import cz.it4i.parallel.Routines;
import cz.it4i.parallel.TestParadigm;

public class RotateFile {

	private static final String OUTPUT_DIRECTORY = "output";

	private final static int step = 1;

	public static void main(String[] args) {
		final Context context = new Context();
		try ( ParallelizationParadigm paradigm = TestParadigm.localImageJServer( Config.getFijiExecutable(), context ) ) {
			callRemotePlugin(paradigm);
		}
	}

	static void callRemotePlugin( final ParallelizationParadigm paradigm ) {
		final List< Map< String, Object > > parametersList = initParameters();
		final List<Map<String, Object>> results = paradigm.runAll(
				RotateImageXY.class, parametersList);
		saveOutputs( parametersList, results );
	}

	static List< Map< String, Object > > initParameters()
	{
		final List<Map<String, Object>> parametersList = new LinkedList<>();
		Path path = ExampleImage.lenaAsTempFile();
		for (double angle = step; angle < 360; angle += step) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("dataset", path);
			parameters.put("angle", angle);
			parametersList.add(parameters);
		}
		return parametersList;
	}

	private static void saveOutputs( List< Map< String, Object > > parametersList, List< Map< String, Object > > results )
	{
		final Path outputDirectory = prepareOutputDirectory();
		final Iterator<Map<String, Object>> inputIterator = parametersList
				.iterator();
		for (Map<String, ?> result : results) {
			final Double angle = ( Double ) inputIterator.next().get( "angle" );
			final Path outputFile = outputDirectory.resolve("result_" + angle +
				".png");
			runWithExceptionHandling( () -> Files.move( ( Path ) result.get( "dataset" ),
				outputFile, StandardCopyOption.REPLACE_EXISTING));
		}
	}

	static Path prepareOutputDirectory() {
		Path outputDirectory = Paths.get(OUTPUT_DIRECTORY);
		if (!Files.exists(outputDirectory)) {
			Routines.runWithExceptionHandling(() -> Files.createDirectories(
				outputDirectory));
		}
		return outputDirectory;
	}
}
