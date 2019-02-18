
package test;

import static test.RotateFile.prepareOutputDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cz.it4i.parallel.TestParadigm;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

public class RotateDataset {

	public static void main(String[] args) throws IOException
	{
		Context context = new Context();
		DatasetIOService ioService = context.service( DatasetIOService.class );
		try( ParallelizationParadigm paradigm = TestParadigm.localImageJServer( Config.getFijiExecutable(), context ))
		{
			List< Map< String, Object > > parametersList = initParameters(ioService);
			List<Map<String, Object>> results = paradigm.runAll(RotateImageXY.class,
					parametersList);
			saveResults( ioService, parametersList, results );
		}
	}

	private static void saveResults( DatasetIOService ioService, List< Map< String, Object > > parametersList, List< Map< String, Object > > results ) throws IOException
	{
		Iterator<Map<String, Object>> inputIterator = parametersList.iterator();
		Path outputDirectory = prepareOutputDirectory();
		for (Map<String, ?> result : results) {
			final Double angle = ( Double ) inputIterator.next().get( "angle" );
			final Path outputFile = outputDirectory.resolve( "result_" + angle + ".tif" );
			ioService.save((Dataset) result.get("dataset"), outputFile.toString());
		}
	}

	private static List< Map< String, Object > > initParameters( DatasetIOService ioService ) throws IOException
	{
		List<Map<String, Object>> parametersList = new LinkedList<>();
		Dataset dataset = ioService.open( ExampleImage.lenaAsTempFile().toString());
		for (double angle = 0; angle < 360; angle += 30) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("dataset", dataset);
			parameters.put("angle", angle);
			parametersList.add(parameters);
		}
		return parametersList;
	}
}
