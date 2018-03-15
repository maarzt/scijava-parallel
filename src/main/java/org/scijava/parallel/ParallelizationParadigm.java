// TODO: Add copyright stuff

package org.scijava.parallel;

import java.util.function.BiConsumer;

import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin {

	void init();

	void submit();

	<T> void parallelLoop(Iterable<T> arguments, BiConsumer<T, ParallelTask> consumer);

	
}