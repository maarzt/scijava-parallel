// TODO: Add copyright stuff

package org.scijava.parallel;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.scijava.command.Command;
import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin, Closeable {

	void init();

	default List<Map<String, ?>> runAll(List<Class<? extends Command>> commands,
		List<Map<String, ?>> parameters) {
		return runAllCommands(commands.stream().map(clazz -> clazz.getName())
			.collect(Collectors.toList()), parameters);
	}

	default List<CompletableFuture<Map<String, ?>>> runAllAsync(
		List<Class<? extends Command>> commands, List<Map<String, ?>> parameters) {
		return runAllCommandsAsync(commands.stream().map(clazz -> clazz.getName())
			.collect(Collectors.toList()), parameters);
	}

	List<Map<String, ?>> runAllCommands(List<String> commands,
		List<Map<String, ?>> parameters);

	List<CompletableFuture<Map<String, ?>>> runAllCommandsAsync(
		List<String> commands, List<Map<String, ?>> parameters);
	
	RemoteDataset createRemoteDataset(URI uri);

	void exportWriteableDataset(WriteableDataset writeableDataset, URI uri);

	// -- Closeable methods --
	@Override
	default public void close() {

	}

}
