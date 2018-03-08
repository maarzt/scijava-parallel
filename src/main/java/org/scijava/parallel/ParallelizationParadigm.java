// TODO: Add copyright stuff

package org.scijava.parallel;

import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin {
	
	void init() throws Exception;
	
	void submit() throws Exception;
	
}
