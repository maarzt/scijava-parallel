
package cz.it4i.parallel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImageJServerRunner extends AbstractImageJServerRunner {

	private Process imageJServerProcess;

	private String fijiExecutable;

	public ImageJServerRunner(String fiji) {
		fijiExecutable = fiji;
	}

	@Override
	public List< Integer > getPorts()
	{
		return Collections.singletonList( 8080 );
	}

	@Override
	public void close() {
		if (imageJServerProcess != null) {
			imageJServerProcess.destroy();
		}
	}

	@Override
	protected void doStartImageJServer(List<String> command) throws IOException {
		String fijiPath = fijiExecutable;
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
