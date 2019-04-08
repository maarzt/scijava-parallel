package cz.it4i.parallel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.imagej.ImageJ;
import net.imagej.server.ImageJServer;
import net.imagej.server.ImageJServerService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmbeddedImageJServerRunner extends AbstractImageJServerRunner {

	private ImageJServer app;
	private ImageJ ij;

	@Override
	public List<Integer> getPorts() {
		return Arrays.asList(8080);
	}

	@Override
	public void close() {
		try {
			app.stop();
			app.join();
		}
		catch (Exception exc) {
			log.warn(exc.getMessage(), exc);
		}

		ij.context().dispose();
	}

	@Override
	protected void doStartImageJServer() throws IOException {
		ij = new ImageJ();
		ij.ui().setHeadless(true);
		app = ij.get(ImageJServerService.class).start(new String[] {});

	}

}
