
package cz.it4i.parallel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	protected void doStartImageJServer() throws IOException {
		String fijiPath = fijiExecutable;
		if (fijiPath == null || !Files.exists(Paths.get(fijiPath))) {
			throw new IllegalArgumentException(
				"Cannot find the specified ImageJ or Fiji executable (" + fijiPath +
					"). The property 'Fiji.executable.path' may not be configured properly in the 'configuration.properties' file.");
		}

		List< String > command = Stream.concat( Stream.of( fijiPath ), AbstractImageJServerRunner.IMAGEJ_SERVER_PARAMETERS.stream() )
				.collect( Collectors.toList() );

		final ProcessBuilder pb = new ProcessBuilder(command).inheritIO();
		imageJServerProcess = pb.start();

	}

}
