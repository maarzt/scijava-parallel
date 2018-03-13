// TODO: Copyright stuff

package org.scijava.parallel;

import java.util.LinkedHashMap;
import java.util.Map;

import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;

// TODO: Description

public abstract class AbstractParallelizationParadigm implements ParallelizationParadigm {

	@Parameter
	protected PrefService prefService;

	protected final Map<String, String> connectionConfig = new LinkedHashMap<>();

	protected void retrieveConnectionConfig() {
		connectionConfig.clear();
		connectionConfig.putAll(prefService.getMap(getClass(), "connectionConfig"));
	}

	protected void updateConnectionConfig(Map<String, String> inputMap) {
		connectionConfig.clear();
		prefService.put(getClass(), "connectionConfig", inputMap);
		connectionConfig.putAll(inputMap);
	}
}