package cz.it4i.parallel;

import java.util.Collection;
import java.util.LinkedList;

import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = ParallelizationParadigm.class)
public class ImageJServerParadigm extends SimpleOstravaParadigm {
	
	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.ImageJServerParadigm.class);
		
	private int port;

	private final Collection<String> hosts = new LinkedList<>();

	// -- ImageJServerParadigm methods --

	public void setPort(int port) {
		this.port = port;
	}

	public void setHosts(Collection<String> hosts) {
		this.hosts.clear();
		this.hosts.addAll(hosts);
	}
	
	// -- SimpleOstravaParadigm methods --

	@Override
	protected void initWorkerPool() {
		hosts.forEach(host -> workerPool.addWorker(new ImageJServerWorker(host, port)));
	}

}
