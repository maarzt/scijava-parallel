
package cz.it4i.parallel;

import java.util.Set;

import org.scijava.parallel.ParallelizationParadigm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractParallelizationParadigmConverterFactory<O>
	implements ParallelizationParadigmConverterFactory<O>
{

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.AbstractParallelizationParadigmConverterFactory.class);

	private Set<Class<? extends ParallelizationParadigm>> supportedParadigms;

	private Class<O> supportedParameterType;

	public AbstractParallelizationParadigmConverterFactory(
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
	public Class<O> getSupportedParameterType() {
		return supportedParameterType;
	}

	abstract protected class AbstractConverter extends
		org.scijava.convert.AbstractConverter<Object, O>
	{

		@Override
		public Class<O> getOutputType() {
			return supportedParameterType;
		}

		@Override
		public Class<Object> getInputType() {
			return Object.class;
		}
	}
}
