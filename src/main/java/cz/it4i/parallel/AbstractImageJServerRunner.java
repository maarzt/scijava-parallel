
package cz.it4i.parallel;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractImageJServerRunner implements AutoCloseable {

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.AbstractImageJServerRunner.class);

	private static String[] IMAGEJ_SERVER_WITH_PARAMETERS = { "ImageJ-linux64",
		"-Dimagej.legacy.modernOnlyCommands=true", "--", "--ij2", "--headless",
		"--server" };

	private List<Integer> ports;

	public void start() {

		try {
			doStartImageJServer(Arrays.asList(IMAGEJ_SERVER_WITH_PARAMETERS));
			imageJServerStarted();
			getPorts().parallelStream().forEach( this::waitForImageJServer );
			imageJServerRunning();
		}
		catch (IOException exc) {
			log.error("start imageJServer", exc);
			throw new RuntimeException(exc);
		}
	}

	public List<Integer> getPorts() {
		return ports;
	}

	protected void setPorts(List<Integer> ports) {
		this.ports = ports;
	}

	@Override
	abstract public void close();

	protected abstract void doStartImageJServer(List<String> command)
		throws IOException;

	protected void imageJServerStarted() {}

	protected void imageJServerRunning() {}

	private void waitForImageJServer( Integer port )
	{
		boolean running = false;
		while (!running) {
			try {
				running = checkModulesURL(port);
			}
			catch (IOException e) {
				// ignore waiting for start
			}
		}
	}

	private boolean checkModulesURL(Integer port) throws IOException
	{
		HttpURLConnection hc;
		hc = (HttpURLConnection) new URL(getModulesURL(port)).openConnection();
		hc.setRequestMethod("GET");
		hc.connect();
		hc.disconnect();
		return hc.getResponseCode() == 200;
	}

	private String getModulesURL(Integer port) {
		return "http://localhost:" + port + "/modules";
	}

}
