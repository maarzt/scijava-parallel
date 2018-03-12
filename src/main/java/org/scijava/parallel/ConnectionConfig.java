// TODO: Add copyright stuff

package org.scijava.parallel;

// TODO: Add description

public class ConnectionConfig {
	
	private final String hostName;
	private final int port;
	
	public ConnectionConfig(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public int getPort() {
		return port;
	}

}
