// TODO: Add copyright stuff

package org.scijava.parallel;

import java.util.List;
import java.util.stream.Collectors;

import org.scijava.plugin.SingletonService;
import org.scijava.service.SciJavaService;

/**
 * A service providing parallelization capabilities
 * 
 * @author TODO: Add authors
 */
public interface ParallelService extends SingletonService<ParallelizationParadigm>, SciJavaService {
	
	/**
	 * Gets all available parallelization paradigms
	 * 
	 * @return A list of available parallelization paradigms
	 */
	// TODO: Consider adding configuration parameters to filter the available paradigms 
	default List<ParallelizationParadigm> getParadigms() {
		return getInstances().stream().collect(Collectors.toList());
	}

}
