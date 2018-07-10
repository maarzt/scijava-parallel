package test.learnathon;

import static test.Config.getOutputFilesPattern;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Routines {
	public static Path constructOutputPath(final Path fileToRotate, final int angle) {
		return Paths.get(getOutputFilesPattern() + angle + suffix(fileToRotate));
	}

	public static String suffix(final Path path) {
		return path.toString().substring(path.toString().lastIndexOf('.'));
	}

}
