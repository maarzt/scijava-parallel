
package cz.it4i.parallel;

import java.util.Map;

import org.scijava.convert.Converter;

public class DefaultParameterProcessor extends ParameterProcessor {

	private Map<Class<?>, ParallelizationParadigmConverterFactory<?>> mappers;

	public DefaultParameterProcessor(ParameterTypeProvider typeProvider,
		String commandName, Object servingWorker,
		Map<Class<?>, ParallelizationParadigmConverterFactory<?>> mappers)
	{
		super(typeProvider, commandName, servingWorker);
		this.mappers = mappers;
	}

	@Override
	protected <T> Converter<Object, T> construcConverter(Class<T> expectedType,
		Object servingWorker)
	{
		@SuppressWarnings("unchecked")
		ParallelizationParadigmConverterFactory<T> result =
			(ParallelizationParadigmConverterFactory<T>) mappers.get(expectedType);
		if (result != null) {
			return result.createConverterForWorker(servingWorker);
		}
		return null;
	}

}
