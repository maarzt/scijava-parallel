package cz.it4i.parallel;

import java.util.function.BiConsumer;

import org.scijava.parallel.AbstractParallelizationParadigm;
import org.scijava.parallel.ParallelTask;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;

import cz.it4i.fiji.haas_java_client.HaaSClient;
import cz.it4i.fiji.haas_java_client.Settings;

@Plugin(type = ParallelizationParadigm.class)
public class HeappeParadigm2 extends AbstractParallelizationParadigm {

	private HaaSClient haasClient;
	
	// -- HeappeParadigm methods --
	
	// -- ParallelizationParadigm methods --
	
	@Override
	public void init() {
		
		haasClient = new HaaSClient(BuildHaasClientSettings());
	}

	@Override
	public <T> void parallelLoop(Iterable<T> arguments, BiConsumer<T, ParallelTask> consumer) {
		// TODO Auto-generated method stub
		
	}
	
	// -- helper methods --
	
	private Settings BuildHaasClientSettings() {
		return new Settings() {
			
			@Override
			public String getUserName() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public int getTimeout() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public long getTemplateId() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public String getProjectId() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getPhone() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getPassword() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getJobName() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getEmail() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public long getClusterNodeType() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int getNumberOfCoresPerNode() {
				// TODO Auto-generated method stub
				return 0;
			}
		};
	}

}
