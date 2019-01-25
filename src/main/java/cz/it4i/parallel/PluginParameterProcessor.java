
package cz.it4i.parallel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.scijava.parallel.ParallelizationParadigm;

public class PluginParameterProcessor extends ParameterProcessor {

	private Class<? extends ParallelizationParadigm> paradigmType;

	private Map<String, ParallelizationParadigmParameterMapper> mappers =
		new HashMap<>();

	public PluginParameterProcessor(
		Class<? extends ParallelizationParadigm> paradigmType,
		ParameterTypeProvider typeProvider, String commandName)
	{
		super(typeProvider, commandName);
		this.paradigmType = paradigmType;
		initMappers();
	}

	@Override
	protected ParallelizationParadigmParameterMapper construcMapper(
		String expectedTypeName)
	{
		ParallelizationParadigmParameterMapper result = mappers.get(
			expectedTypeName);
		if (result != null) {
			return result.clone();
		}
		return null;
	}

	private void initMappers() {
		getMappers().stream().filter(m -> isParadigmSupportedBy(m)).forEach(m -> m
			.getSupportedParameterTypeNames().stream().forEach(name -> mappers.put(
				name, m)));

	}

	private boolean isParadigmSupportedBy(
		ParallelizationParadigmParameterMapper m)
	{
		for (Class<? extends ParallelizationParadigm> clazz : m
			.getSupportedParadigms())
		{
			if (clazz.isAssignableFrom(paradigmType)) {
				return true;
			}
		}
		return false;
	}

	private Collection<ParallelizationParadigmParameterMapper> getMappers() {
		// TODO Auto-generated method stub
		return null;
	}

}
