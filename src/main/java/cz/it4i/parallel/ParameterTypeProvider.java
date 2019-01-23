package cz.it4i.parallel;


public interface ParameterTypeProvider {
	
	String provideParameterTypeName(String commandName, String parameterName);
}
