package cz.it4i.parallel;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class ParameterProcessor {
	interface ConversionsProviding {
		Object convertInput(Object input);
		Object convertOutput(Object input);
	}
	
	private Map<String,ConversionsProviding> appliedConversions = new HashMap<>(); 
	
	private String commandName;

	private ParameterTypeProvider typeProvider;
	
	public ParameterProcessor(ParameterTypeProvider typeProvider ,String commandName) {
		this.commandName = commandName;
		this.typeProvider = typeProvider;
	}

	public Map<String,Object> processInputs(Map<String,Object> inputs) {
		Map<String,Object> result = new HashMap<>();
		for(Entry<String,Object> entry: inputs.entrySet()) {
			result.put(entry.getKey(), doInputConversion(entry));
		}
		return result;
	}
	
	public Map<String,Object> processOutput(Map<String,Object> inputs) {
		Map<String,Object> result = new HashMap<>();
		for(Entry<String,Object> entry: inputs.entrySet()) {
			result.put(entry.getKey(), doOutputConversion(entry));
		}
		return result;
	}
	
	protected String getCommandName() {
		return commandName;
	}
	
	abstract protected  ConversionsProviding getConvertor(String askedCommandName, String expectedTypeName);

	private String getParameterTypeName(String parameter) {
		return typeProvider.provideParameterTypeName(commandName, parameter);
	}

	
	private Object doInputConversion(Entry<String,Object> parameter)
	{
		ConversionsProviding convertor = getConvertor(commandName ,getParameterTypeName(parameter.getKey()));
		Object value = parameter.getValue();
		if (convertor != null) {
			appliedConversions.put(parameter.getKey(), convertor);
			value = convertor.convertInput(parameter.getValue());
			
		}
		return value;
	}

	private Object doOutputConversion(Entry<String,Object> parameter)
	{
		Object value = parameter.getValue();
		ConversionsProviding convertor = appliedConversions.get(parameter.getKey());
		if (convertor != null) {
			value = convertor.convertOutput(value);
		}
		return value;
	}
	
}
