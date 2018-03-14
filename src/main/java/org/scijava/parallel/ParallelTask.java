package org.scijava.parallel;

import java.util.Map;

public interface ParallelTask {

	
	<T> T getRemoteModule(Class<T> type);
	
	<T>void run(Class<T> type, Map<String, Object> inputMap);

}
