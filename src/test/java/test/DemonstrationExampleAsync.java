package test;

import com.google.common.collect.Streams;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.WriteableDataset;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = true)
public class DemonstrationExampleAsync extends DemonstrationExample{

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrationExampleAsync.class, true);
	}
	
	@Override
	protected void doRotation(ParallelizationParadigm paradigm) {
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
}