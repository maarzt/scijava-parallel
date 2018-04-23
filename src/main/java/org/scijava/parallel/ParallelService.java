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

	@SuppressWarnings("unchecked")
	default <T extends ParallelizationParadigm> T getParadigm(
			Class<T> desiredParalellizationParadigm) {
		List<ParallelizationParadigm> matchingParadigms = getInstances().stream()
				.filter(paradigm -> paradigm.getClass().equals(desiredParalellizationParadigm))
				.collect(Collectors.toList());
		
		if (matchingParadigms.size() == 1) {
			return (T) matchingParadigms.get(0);
		}
		
		return null;
	}

	@Override
	default Class<ParallelizationParadigm> getPluginType() {
		return ParallelizationParadigm.class;
	}
}
