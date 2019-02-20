// TODO: Add copyright stuff

package org.scijava.parallel;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.scijava.command.Command;
import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin, Closeable {

	void init();

	default List<Map<String, Object>> runAll(
		Class<? extends Command> commandClazz, List<Map<String, Object>> parameters)
	{
		return runAll(commandClazz.getName(), parameters);
	}

	default List<CompletableFuture<Map<String, Object>>> runAllAsync(
		Class<? extends Command> commandClazz, List<Map<String, Object>> parameters)
	{
		return runAllAsync(commandClazz.getName(), parameters);
	}

	List<Map<String, Object>> runAll(String commandName,
		List<Map<String, Object>> parameters);

	List<CompletableFuture<Map<String, Object>>> runAllAsync(String commandName,
		List<Map<String, Object>> parameters);

	// -- Closeable methods --
	@Override
	default public void close() {

	}

}
