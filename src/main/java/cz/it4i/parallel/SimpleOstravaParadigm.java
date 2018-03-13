package cz.it4i.parallel;

import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;

@Plugin(type = ParallelizationParadigm.class)
public class SimpleOstravaParadigm extends AbstractParallelizationParadigm {

	@Override
	public void init() {		
		
		// Consider moving to AbstractParallelizationParadigm
		
		int abc = 123;

	}

	@Override
	public void submit() {
		// TODO Auto-generated method stub, consider moving to
		// AbstractParallelizationParadigm

	}

}
