package cz.it4i.parallel;

import java.nio.file.Files;
import java.nio.file.Path;

import net.imagej.Dataset;

import org.scijava.io.IOService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestParameterProcessor extends ParameterProcessor {

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.TestParameterProcessor.class);
	
	private ParallelWorker pw;

	private IOService ioService;

	public TestParameterProcessor(IOService ioService ,ParameterTypeProvider typeProvider,ParallelWorker pw, String command) {
		super(typeProvider, command);
		this.pw = pw;
		this.ioService = ioService;
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

		private String suffixOfImportedFile;

		private Dataset workingDataSet;
		
		private Path tempFileForWorkingDataSet;
		
		@Override
		public Object convertInput(Object input) {
			if (input instanceof Path) {
				Path path = (Path) input;
				String filename = path.getFileName().toString();
				suffixOfImportedFile = getSuffix(filename);
				return pw.importData(path);	
			} else if (input instanceof Dataset) {
				workingDataSet = (Dataset) input;
				String workingSuffix = getSuffix(workingDataSet.getName());
				tempFileForWorkingDataSet = Routines.supplyWithExceptionHandling(() -> Files.createTempFile("", workingSuffix), log, "convertInput");
				Routines.runWithExceptionHandling(() -> ioService.save(input, tempFileForWorkingDataSet.toString()),log, "convertInput");
				return pw.importData(tempFileForWorkingDataSet);
			}
			throw new IllegalArgumentException("cannot convert from " + input.getClass());
		}

		private String getSuffix(String filename) {
			return filename.substring(filename.lastIndexOf('.'), filename.length());
		}

		@Override
		public Object convertOutput(Object input) {
			if (suffixOfImportedFile != null) {
				Path result = Routines.supplyWithExceptionHandling(() -> Files.createTempFile("", suffixOfImportedFile), log, "output conversion");
				pw.exportData(input, result);
				pw.deleteData(input);
				return result;
			} else if (workingDataSet != null && tempFileForWorkingDataSet != null) {
				pw.exportData(input, tempFileForWorkingDataSet);
				Dataset tempDataset = (Dataset) Routines.supplyWithExceptionHandling(() -> ioService.open(tempFileForWorkingDataSet.toString()), log, "convertOutput");
				tempDataset.copyDataFrom(workingDataSet);
				Routines.runWithExceptionHandling(() -> Files.delete(tempFileForWorkingDataSet), log, "");
				return workingDataSet;
			}
			throw new IllegalArgumentException("bad arguments");
		}
		
	}
	
}
