
package cz.it4i.parallel;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class ParameterProcessor {

	private Map<String, ParallelizationParadigmParameterMapper> appliedConversions =
		new HashMap<>();

	private String commandName;

	private ParameterTypeProvider typeProvider;

	private Object worker;

	public ParameterProcessor(ParameterTypeProvider typeProvider,
		String commandName, Object worker)
	{
		this.commandName = commandName;
		this.typeProvider = typeProvider;
		this.worker = worker;

	}

	public Map<String, Object> processInputs(Map<String, Object> inputs) {
		Map<String, Object> result = new HashMap<>();
		for (Entry<String, Object> entry : inputs.entrySet()) {
			result.put(entry.getKey(), doInputConversion(entry));
		}
		return result;
	}

	public Map<String, Object> processOutput(Map<String, Object> inputs) {
		Map<String, Object> result = new HashMap<>();
		for (Entry<String, Object> entry : inputs.entrySet()) {
			result.put(entry.getKey(), doOutputConversion(entry));
		}
		return result;
	}

	protected String getCommandName() {
		return commandName;
	}

	abstract protected ParallelizationParadigmParameterMapper construcMapper(
		String expectedTypeName, Object servingWorker);

	private String getParameterTypeName(String parameter) {
		return typeProvider.provideParameterTypeName(commandName, parameter);
	}

	private Object doInputConversion(Entry<String, Object> parameter) {
		ParallelizationParadigmParameterMapper convertor = construcMapper(
			getParameterTypeName(parameter.getKey()), worker);
		Object value = parameter.getValue();
		if (convertor != null) {
			appliedConversions.put(parameter.getKey(), convertor);
			value = convertor.map2Paradigm(parameter.getValue());

		}
		return value;
	}

	private Object doOutputConversion(Entry<String, Object> parameter) {
		Object value = parameter.getValue();
		ParallelizationParadigmParameterMapper convertor = appliedConversions.get(
			parameter.getKey());
		if (convertor != null) {
			value = convertor.map2Local(value);
		}
		return value;
	}

}
