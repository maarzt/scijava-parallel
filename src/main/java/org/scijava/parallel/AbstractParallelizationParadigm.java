// TODO: Copyright stuff

package org.scijava.parallel;

import org.scijava.plugin.Parameter;
import org.scijava.prefs.PrefService;

// TODO: Description

public abstract class AbstractParallelizationParadigm implements ParallelizationParadigm {
	
	@Parameter
	protected PrefService prefService;
	
	protected ConnectionConfig connectionConfig;

}