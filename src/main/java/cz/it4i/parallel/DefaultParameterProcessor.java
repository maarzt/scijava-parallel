
package cz.it4i.parallel;

import java.util.Map;

public class DefaultParameterProcessor extends ParameterProcessor {

	private Map<String, ParallelizationParadigmParameterMapper> mappers;

	public DefaultParameterProcessor(ParameterTypeProvider typeProvider,
		String commandName, Object servingWorker,
		Map<String, ParallelizationParadigmParameterMapper> mappers)
	{
		super(typeProvider, commandName, servingWorker);
		this.mappers = mappers;
	}

	@Override
	protected ParallelizationParadigmParameterMapper construcMapper(
		String expectedTypeName, Object servingWorker)
	{
		ParallelizationParadigmParameterMapper result = mappers.get(
			expectedTypeName);
		if (result != null) {
			return result.cloneForWorker(servingWorker);
		}
		return null;
	}

}
