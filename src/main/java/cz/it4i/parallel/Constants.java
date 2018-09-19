
package cz.it4i.parallel;

public interface Constants {

	int WALLTIME = 3600;
	long CLUSTER_NODE_TYPE = 7l;
	String PROJECT_ID = "open-12-20";
	String CONFIG_FILE_NAME = "configuration.properties";
	int NUMBER_OF_CORE = 24;

	interface HEAppE {

		String JOB_NAME = "ImageJ-Server-HEAppE";
		long TEMPLATE_ID = 4;
		String RUN_IJS = "run-ijs";
	}
}
