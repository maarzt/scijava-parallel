// TODO: Add copyright stuff

package org.scijava.parallel;

import java.util.function.BiConsumer;

import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin {
	
	void init();
	
	<T> void parallelFor(Iterable<T> arguments, BiConsumer<T, ExecutionContext> consumer);
}
