package cz.it4i.parallel;

import java.util.LinkedHashMap;
import java.util.Map;

import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;

@Plugin(type = ParallelizationParadigm.class)
public class SimpleOstravaParadigm extends AbstractParallelizationParadigm {

	@Override
	public void init() {
		
		retrieveConnectionConfig();
		if (connectionConfig.size() == 0) {
			Map<String, String> configEntries = new LinkedHashMap<>();
			configEntries.put("address", "localhost");
			configEntries.put("port", "8080");
			updateConnectionConfig(configEntries);
		}

	}

	@Override
	public void submit() {
		// TODO Auto-generated method stub, consider moving to
		// AbstractParallelizationParadigm

	}

}
