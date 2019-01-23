package cz.it4i.parallel;

import java.nio.file.Files;
import java.nio.file.Path;

import net.imagej.Dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestParameterProcessor extends ParameterProcessor {

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.TestParameterProcessor.class);
	
	private ParallelWorker pw;

	public TestParameterProcessor(ParameterTypeProvider typeProvider,ParallelWorker pw, String command) {
		super(typeProvider, command);
		this.pw = pw;
	}

	@Override
	protected ConversionsProviding getConvertor(String commandName,
		String expectedType)
	{
		if (Dataset.class.getName().equals(expectedType)) {
			return new P_DatasetConvertor();
		}
		return null;
	}

	

	private class P_DatasetConvertor implements ConversionsProviding{

		private String suffix;
		
		@Override
		public Object convertInput(Object input) {
			if (input instanceof Path) {
				Path path = (Path) input;
				String filename = path.getFileName().toString();
				suffix = filename.substring(filename.lastIndexOf('.'), filename.length());
				return pw.importData(path);	
			}
			throw new IllegalArgumentException("cannot convert from " + input.getClass());
		}

		@Override
		public Object convertOutput(Object input) {
			Path result = Routines.supplyWithExceptionHandling(() -> Files.createTempFile("", suffix), log, "output conversion");
			pw.exportData(input, result);
			pw.deleteData(input);
			return result;
		}
		
	}
	
}
