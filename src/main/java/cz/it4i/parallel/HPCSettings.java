package cz.it4i.parallel;

import com.google.common.base.Strings;

import java.io.File;

import lombok.Builder;
import lombok.Data;

@Data

@Builder
public class HPCSettings
{
	private final String host;

	private final String userName;

	private final File keyFile;

	private final String keyFilePassword;

	private final String remoteDirectory;

	private final String command;

	private final int nodes;

	private final int ncpus;

	private final String jobID;

	private final boolean shutdownJobAfterClose;

}
