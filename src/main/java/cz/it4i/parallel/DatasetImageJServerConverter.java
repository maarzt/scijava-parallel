
package cz.it4i.parallel;

import static cz.it4i.parallel.Routines.castTo;
import static cz.it4i.parallel.Routines.getSuffix;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import net.imagej.Dataset;

import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = ParallelizationParadigmConverter.class)
public class DatasetImageJServerConverter extends
	AbstractParallelizationParadigmConverter<Dataset> implements Closeable
{

	@Parameter
	private IOService ioService;

	private ParallelWorker parallelWorker;

	private String suffixOfImportedFile;

	private Dataset workingDataSet;

	private Path tempFileForWorkingDataSet;

	public DatasetImageJServerConverter() {
		super(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			ImageJServerParadigm.class))), Dataset.class);
	}

	@Override
	public ParallelizationParadigmConverter<Dataset> cloneForWorker(
		ParallelWorker worker)
	{
		DatasetImageJServerConverter result =
			new DatasetImageJServerConverter();
		result.ioService = ioService;
		result.parallelWorker = worker;
		return result;
	}

	@Override
	public <T> T convert(Object src, Class<T> dest) {
		if (dest == Object.class) {
			return castTo(convert2Paradigm(src));
		}
		return castTo(convert2Local(src));
	}
	
	@Override
	public void close() throws IOException {
		if (null != tempFileForWorkingDataSet) {
			Files.deleteIfExists(tempFileForWorkingDataSet);
			tempFileForWorkingDataSet = null;
		}
	}

	private Object convert2Paradigm(Object input) {
		if (input instanceof Path) {
			Path path = (Path) input;
			String filename = path.getFileName().toString();
			suffixOfImportedFile = getSuffix(filename);
			return parallelWorker.importData(path);
		}
		else if (input instanceof Dataset) {
			workingDataSet = (Dataset) input;
			String workingSuffix = getSuffix(workingDataSet.getName());
			tempFileForWorkingDataSet = Routines.supplyWithExceptionHandling(
				() -> Files.createTempFile(Thread.currentThread().toString(),
					workingSuffix));
			Routines.runWithExceptionHandling(() -> ioService.save(input,
				tempFileForWorkingDataSet.toString()));
			return parallelWorker.importData(tempFileForWorkingDataSet);
		}
		throw new IllegalArgumentException("cannot convert from " + input
			.getClass());
	}

	private Object convert2Local(Object input) {
		if (suffixOfImportedFile != null) {
			Path result = Routines.supplyWithExceptionHandling(() -> Files
				.createTempFile("", suffixOfImportedFile));
			parallelWorker.exportData(input, result);
			parallelWorker.deleteData(input);
			return result;
		}
		else if (workingDataSet != null && tempFileForWorkingDataSet != null) {
			parallelWorker.exportData(input, tempFileForWorkingDataSet);
			parallelWorker.deleteData(input);
			Dataset tempDataset = (Dataset) Routines.supplyWithExceptionHandling(
				() -> ioService.open(tempFileForWorkingDataSet.toString()));
			tempDataset.copyInto(workingDataSet);
			Routines.runWithExceptionHandling(() -> Files.delete(
				tempFileForWorkingDataSet));
			tempFileForWorkingDataSet = null;
			return workingDataSet;
		}
		throw new IllegalArgumentException("bad arguments");
	}

}
