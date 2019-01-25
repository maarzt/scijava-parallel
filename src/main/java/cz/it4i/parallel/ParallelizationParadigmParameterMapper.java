
package cz.it4i.parallel;

import java.util.Set;

import org.scijava.parallel.ParallelizationParadigm;

public interface ParallelizationParadigmParameterMapper {

	Set<Class<? extends ParallelizationParadigm>> getSupportedParadigms();

	Set<String> getSupportedParameterTypeNames();

	Object map2Paradigm(Object input);

	Object map2Local(Object input);

	ParallelizationParadigmParameterMapper cloneForWorker(Object worker);
}
