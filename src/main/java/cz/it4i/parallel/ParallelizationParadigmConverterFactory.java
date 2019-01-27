
package cz.it4i.parallel;

import java.util.Set;

import org.scijava.convert.Converter;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.SciJavaPlugin;

public interface ParallelizationParadigmConverterFactory<O> extends
	SciJavaPlugin
{

	Set<Class<? extends ParallelizationParadigm>> getSupportedParadigms();

	Class<O> getSupportedParameterType();

	Converter<Object, O> createConverterForWorker(Object worker);

}
