package test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageJServerRunner implements AutoCloseable{

	private final static Logger log = LoggerFactory.getLogger(
		test.ImageJServerRunner.class);
	
	private static final String MEODULES_URL = "http://localhost:8080/modules";
	
	private static String[] IMAGEJ_SERVER_WITH_PARAMETERS = { "ImageJ-linux64",
		"-Dimagej.legacy.modernOnlyCommands=true", "--", "--ij2", "--headless",
		"--server" };

	private Process imageJServerProcess;

	
	public ImageJServerRunner() {
		startImageJServerIfNecessary();
	}
	
	@Override
	public void close() {
		if (imageJServerProcess != null) {
			imageJServerProcess.destroy();
		}
	}

	private ImageJServerRunner startImageJServerIfNecessary() {
		if (!checkImageJServerRunning()) {
			startImageJServer();
		}
		return this;
	}

	private void startImageJServer() {
		boolean running = false;
		String[] command = IMAGEJ_SERVER_WITH_PARAMETERS.clone();
		command[0] = Paths.get(Config.getFijiLocation(), command[0]).toString();
		try {
			ProcessBuilder pb = new ProcessBuilder(command).inheritIO();
			imageJServerProcess =  pb.start();
			do {
				try {
					if(checkModulesURL() == 200) {
						running = true;
					}
				} catch (IOException e) {
					//ignore waiting for start
				}
			} while(!running);
		}
		catch (IOException exc) {
			log.error("start imageJServer", exc);
			throw new RuntimeException(exc);
		}
	}

	private boolean checkImageJServerRunning() {
		boolean running = true;
		try {
			if ( checkModulesURL() != 200) {
				throw new IllegalStateException("Different server than ImageJServer is running on localhost:8080");
			}
		}
		catch (ConnectException exc) {
			running = false;
		}
		catch (IOException exc) {
			log.error("connect ot ImageJServer", exc);
			throw new RuntimeException(exc);
		}
		return running;
	}

	private int checkModulesURL() throws IOException, MalformedURLException,
		ProtocolException
	{
		HttpURLConnection hc;
		hc = (HttpURLConnection) new URL(MEODULES_URL).openConnection();
		hc.setRequestMethod("GET");
		hc.connect();
		hc.disconnect();
		return hc.getResponseCode();
	}

}
