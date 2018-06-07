package test.learnathon;

import static test.Config.getOutputFilesPattern;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Routines {
	public static Path constructOutputPath(Path fileToRotate, int angle) {
		return Paths.get(getOutputFilesPattern() + angle + suffix(fileToRotate));
	}

	public static String suffix(Path path) {
		return path.toString().substring(path.toString().lastIndexOf('.'));
	}

}
