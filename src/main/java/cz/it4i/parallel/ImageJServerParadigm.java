package cz.it4i.parallel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Plugin(type = ParallelizationParadigm.class)
public class ImageJServerParadigm extends SimpleOstravaParadigm {

	
	private static final String PORT = "port";


	public static final Logger log = LoggerFactory.getLogger(cz.it4i.parallel.ImageJServerParadigm.class);

	private final Collection<String> hosts = new LinkedList<>();

	@Override
	public void init() {
		if (poolSize == null) {
			poolSize = Math.max(hosts.size(), 1);
		}
		// Unused persistence code, to be revived later
		retrieveConnectionConfig();
		if (connectionConfig.size() == 0) {
			Map<String, String> configEntries = new LinkedHashMap<>();
			//configEntries.put(ADDRESS, "localhost");
			configEntries.put(PORT, "8080");
			updateConnectionConfig(configEntries);
		} 
		super.init();
	}

	public void setHosts(Collection<String> hosts) {
		this.hosts.clear();
		this.hosts.addAll(hosts);

	}

	@Override
	protected void initWorkerPool() {
		hosts.forEach(host -> workerPool
				.addWorker(new ImageJServerWorker(host, Integer.parseInt(connectionConfig.get(PORT)))));

	}
	
		
}
