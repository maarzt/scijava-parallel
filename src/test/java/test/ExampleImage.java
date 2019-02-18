package test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static cz.it4i.parallel.Routines.getSuffix;

public class ExampleImage
{

	private final static Logger log = LoggerFactory.getLogger(RotateFile.class);

	static Path lenaAsTempFile()
	{
		return downloadToTmpFile( "https://upload.wikimedia.org/wikipedia/en/7/7d/Lenna_%28test_image%29.png" );
	}

	private static Path downloadToTmpFile(String url) {
		try (InputStream is = new URL(url).openStream()) {
			final File tempFile = File.createTempFile( "tmp", getSuffix(url) );
			tempFile.deleteOnExit();
			Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return tempFile.toPath();
		}
		catch (IOException exc) {
			log.error("download image", exc);
			throw new RuntimeException(exc);
		}
	}
}
