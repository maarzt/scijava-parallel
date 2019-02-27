
package cz.it4i.parallel;

import java.util.Set;

import org.scijava.convert.Converter;
import org.scijava.parallel.ParallelizationParadigm;

public interface ParallelizationParadigmConverter<O> extends
	Converter<Object, O>
{

	Set<Class<? extends ParallelizationParadigm>> getSupportedParadigms();



	Converter<Object, O> cloneForWorker(ParallelWorker worker);

}
