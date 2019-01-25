
package cz.it4i.parallel;

import java.util.Set;

import org.scijava.parallel.ParallelizationParadigm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractMapper implements
	ParallelizationParadigmParameterMapper, Cloneable
{

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.AbstractMapper.class);

	private Set<Class<? extends ParallelizationParadigm>> supportedParadigms;

	private Set<String> supportedParameterTypeNames;

	public AbstractMapper(
		Set<Class<? extends ParallelizationParadigm>> supportedParadigms,
		Set<String> supportedParameterTypeNames)
	{
		this.supportedParadigms = supportedParadigms;
		this.supportedParameterTypeNames = supportedParameterTypeNames;
	}

	@Override
	public Set<Class<? extends ParallelizationParadigm>> getSupportedParadigms() {
		return supportedParadigms;
	}

	@Override
	public Set<String> getSupportedParameterTypeNames() {
		return supportedParameterTypeNames;
	}

	@Override
	public ParallelizationParadigmParameterMapper clone() {
		return (ParallelizationParadigmParameterMapper) Routines
			.supplyWithExceptionHandling(() -> super.clone(), log, "clone");
	}

}
