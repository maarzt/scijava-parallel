// TODO: Add copyright stuff

package org.scijava.parallel;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin, Closeable {

	void init();

	List<Map<String,Object>> runAll(List<Class<?>> commands, List<Map<String,?>> parameters);
	
	ExecutionContext createExecutionContext();
	
	// -- Closeable methods --

	
	
	@Override
	default public void close() {

	}
}
