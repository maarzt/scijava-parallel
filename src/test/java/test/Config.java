package test;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

	public static final String JPG_SUFFIX = ".jpg";

	public static final String PNG_SUFFIX = ".png";
	
	public static final Logger log = LoggerFactory.getLogger(test.Config.class);
	
	static Properties properties;
	
	static final String CONFIG_FILE_NAME = "config.properties";
	static {
		properties = new Properties();
		try {
			properties.load(Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME));
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public static String getOutputFilesPattern() {
		return properties.getProperty("output_file_patterns");
	}
	
	public static String getInputDirectory() {
		return properties.getProperty("input_directory");
	}
	
	public static String getResultFile() {
		return properties.getProperty("result_file");
	}
}
