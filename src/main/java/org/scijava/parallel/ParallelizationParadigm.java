// TODO: Add copyright stuff

package org.scijava.parallel;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.scijava.command.Command;
import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin, Closeable {

	void init();

	List<Map<String, ?>> runAll(List<Class<? extends Command>> commands,
		List<Map<String, ?>> parameters);

	List<CompletableFuture<Map<String, ?>>> runAllAsync(
		List<Class<? extends Command>> commands, List<Map<String, ?>> parameters);

	RemoteDataset createRemoteDataset(URI uri);

	void exportWriteableDatased(WriteableDataset writeableDataset, URI uri);

	// -- Closeable methods --
	@Override
	default public void close() {

	}

}
