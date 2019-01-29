
package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.it4i.parallel.AbstractImageJServerRunner;

public class ImageJServerRunner extends AbstractImageJServerRunner {

	private Process imageJServerProcess;

	public ImageJServerRunner() {
		setPorts(Arrays.asList(8080));
	}

	@Override
	public void close() {
		if (imageJServerProcess != null) {
			imageJServerProcess.destroy();
		}
	}

	@Override
	protected void doStartImageJServer(List<String> command) throws IOException {
		String fijiPath = Config.getFijiExecutable();
		if (fijiPath == null || !Files.exists(Paths.get(fijiPath))) {
			throw new IllegalArgumentException(
				"Cannot find the specified ImageJ or Fiji executable (" + fijiPath +
					"). The property 'Fiji.executable.path' may not be configured properly in the 'configuration.properties' file.");
		}

		command = new ArrayList<>(command);
		command.set(0, fijiPath);

		final ProcessBuilder pb = new ProcessBuilder(command).inheritIO();
		imageJServerProcess = pb.start();

	}

}
