// TODO: Add copyright stuff

package org.scijava.parallel;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.scijava.command.Command;
import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin, Closeable {

	void init();

	default List<Map<String, Object>> runAll(List<Class<? extends Command>> commands,
		List<Map<String, Object>> parameters) {
		return runAllCommands(commands.stream().map(clazz -> clazz.getName())
			.collect(Collectors.toList()), parameters);
	}

	default List<CompletableFuture<Map<String, Object>>> runAllAsync(
		List<Class<? extends Command>> commands, List<Map<String, Object>> parameters) {
		return runAllCommandsAsync(commands.stream().map(clazz -> clazz.getName())
			.collect(Collectors.toList()), parameters);
	}

	List<Map<String, Object>> runAllCommands(List<String> commands,
		List<Map<String, Object>> parameters);

	List<CompletableFuture<Map<String, Object>>> runAllCommandsAsync(
		List<String> commands, List<Map<String, Object>> parameters);
	
	// -- Closeable methods --
	@Override
	default public void close() {

	}

}
