// TODO: Add copyright stuff

package org.scijava.parallel;

import java.io.Closeable;
import java.util.function.BiConsumer;

import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin, Closeable {
	
	void init();
	
	@Override
	default public void close() {
		
	}
	
	<T> void parallelLoop(Iterable<T> arguments, BiConsumer<T, ParallelTask> consumer);
}
