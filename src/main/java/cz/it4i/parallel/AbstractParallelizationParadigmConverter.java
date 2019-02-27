
package cz.it4i.parallel;

import java.util.Set;

import org.scijava.convert.AbstractConverter;
import org.scijava.parallel.ParallelizationParadigm;

abstract public class AbstractParallelizationParadigmConverter<O> extends
	AbstractConverter<Object, O> implements ParallelizationParadigmConverter<O>,
	Cloneable
{

	private Set<Class<? extends ParallelizationParadigm>> supportedParadigms;

	private Class<O> supportedParameterType;

	public AbstractParallelizationParadigmConverter(
		Set<Class<? extends ParallelizationParadigm>> supportedParadigms,
		Class<O> supportedType)
	{
		this.supportedParadigms = supportedParadigms;
		this.supportedParameterType = supportedType;
	}

	@Override
	public Set<Class<? extends ParallelizationParadigm>> getSupportedParadigms() {
		return supportedParadigms;
	}

	@Override
	public Class<O> getOutputType() {
		return supportedParameterType;
	}

	@Override
	public Class<Object> getInputType() {
		return Object.class;
	}
}
