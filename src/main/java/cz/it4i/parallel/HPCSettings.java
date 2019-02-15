package cz.it4i.parallel;

import java.io.File;

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

	public HPCSettings(String host, String userName, File keyFile,
			String keyFilePassword, String remoteDirectory, String command, int nodes,
			int ncpus)
	{
		super();
		this.host = host;
		this.userName = userName;
		this.keyFile = keyFile;
		this.keyFilePassword = keyFilePassword;
		this.remoteDirectory = remoteDirectory;
		this.command = command;
		this.nodes = nodes;
		this.ncpus = ncpus;
	}

	public String getHost()
	{
		return host;
	}

	public String getUserName()
	{
		return userName;
	}

	public File getKeyFile()
	{
		return keyFile;
	}

	public String getKeyFilePassword()
	{
		return keyFilePassword;
	}

	public String getRemoteDirectory()
	{
		return remoteDirectory;
	}

	public String getCommand()
	{
		return command;
	}

	public int getNodes()
	{
		return nodes;
	}

	public int getNcpus()
	{
		return ncpus;
	}
}
