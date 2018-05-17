package cz.it4i.parallel;

import java.util.Collection;
import java.util.LinkedList;

import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = ParallelizationParadigm.class)
public class ImageJServerParadigm extends SimpleOstravaParadigm {
	
	@Parameter
	private int port;

	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.ImageJServerParadigm.class);

	private final Collection<String> hosts = new LinkedList<>();

	@Override
	public void init() {
		if (poolSize == null) {
			poolSize = Math.max(hosts.size(), 1);
		}		
		super.init();
	}
	
	public void setConnectionConfig(int port) {
		this.port = port;
	}

	public void setHosts(Collection<String> hosts) {
		this.hosts.clear();
		this.hosts.addAll(hosts);
	}

	@Override
	protected void initWorkerPool() {
		hosts.forEach(host -> workerPool.addWorker(new ImageJServerWorker(host, port)));
	}
}
