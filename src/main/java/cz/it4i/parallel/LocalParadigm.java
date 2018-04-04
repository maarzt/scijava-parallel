package cz.it4i.parallel;

import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = ParallelizationParadigm.class)
public class LocalParadigm extends SimpleOstravaParadigm {

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.LocalParadigm.class);

	@Override
	public void init() {
		if (poolSize == null) {
			poolSize = 1;
		}
		super.init();
	}

	@Override
	protected void initWorkerPool() {
		for (int i = 0; i < poolSize; i++) {
			workerPool.addWorker(new LocalPluginWorker());
		}
	}
}
