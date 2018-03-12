// TODO: Add copyright stuff

package org.scijava.parallel;

import org.scijava.plugin.SingletonPlugin;

// TODO: Add description

public interface ParallelizationParadigm extends SingletonPlugin {
		
	boolean init();
	
	void submit();
	
}
