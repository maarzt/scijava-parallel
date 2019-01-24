package test;

import static cz.it4i.parallel.Routines.runWithExceptionHandling;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.plugins.commands.imglib.RotateImageXY;

import org.scijava.command.Command;
import org.scijava.io.IOService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import cz.it4i.parallel.Routines;

@Plugin(type = Command.class, headless = true)
public class DemonstrationExampleDataset extends DemonstrationExample{

	@Parameter
	private IOService ioService;
	
	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrationExampleDataset.class, true);
	}
	
	@Override
	protected void doRotation(ParallelizationParadigm paradigm) {
		Path outputDirectory = prepareoutputDirectory();
		List<Map<String, Object>> parametersList = new LinkedList<>();
		List<Class<? extends Command>> commands = new LinkedList<>();
		initParameters(commands,parametersList);
		
		List<Map<String, Object>> results = paradigm.runAll(commands, parametersList);
		Iterator<Map<String,Object>> inputIterator = parametersList.iterator();
		for(Map<String,?> result: results) {
			runWithExceptionHandling(
				() -> ioService.save(result.get("dataset")
												, getResultPath(outputDirectory,(Double) inputIterator.next().get("angle")).toString())
				,log, "moving file");
		}
	}
	
	
	@Override
	protected void initParameters(List<Class<? extends Command>> commands,
		List<Map<String, Object>> parametersList)
	{
		Path path = getImagetToRotate();
		for (double angle = step; angle < 360; angle += step) {
			commands.add(RotateImageXY.class);
			Map<String, Object> parameters = new HashMap<>();
			Dataset dataset = (Dataset) Routines.supplyWithExceptionHandling(() -> ioService.open(path.toString()), log, "load");
			parameters.put("dataset", dataset);
			parameters.put("angle", angle);
			parametersList.add(parameters);
		}
	}
}