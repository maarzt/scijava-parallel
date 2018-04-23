// TODO: Add copyright stuff

package org.scijava.parallel;

import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;

// TODO: Add description

@Plugin(type = Service.class)
public class DefaultParallelService extends AbstractSingletonService<ParallelizationParadigm>
		implements ParallelService {
	
	@Override
	public void initialize() {	
		super.initialize();		
	}	
}
