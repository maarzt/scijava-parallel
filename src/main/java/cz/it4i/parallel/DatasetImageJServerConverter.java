
package cz.it4i.parallel;

import static cz.it4i.parallel.Routines.castTo;
import static cz.it4i.parallel.Routines.getSuffix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import net.imagej.Dataset;

import org.scijava.convert.Converter;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = ParallelizationParadigmConverter.class)
public class DatasetImageJServerConverter extends
	AbstractParallelizationParadigmConverter<Dataset>
{

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.DatasetImageJServerConverter.class);

	@Parameter
	private IOService ioService;

	public DatasetImageJServerConverter() {
		super(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			ImageJServerParadigm.class))), Dataset.class);
	}

	@Override
	public Converter<Object, Dataset> cloneForWorker(ParallelWorker worker) {
		DatasetImageJServerConverter result =
			new DatasetImageJServerConverter();
		result.ioService = ioService;
		result.parallelWorker = worker;
		return result;
	}



		private ParallelWorker parallelWorker;

		private String suffixOfImportedFile;

		private Dataset workingDataSet;

		private Path tempFileForWorkingDataSet;



		@Override
		public <T> T convert(Object src, Class<T> dest) {
			if (dest == Object.class) {
				return castTo(convert2Paradigm(src));
			}
			return castTo(convert2Local(src));
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
					() -> Files.createTempFile("", workingSuffix), log, "convertInput");
				Routines.runWithExceptionHandling(() -> ioService.save(input,
					tempFileForWorkingDataSet.toString()), log, "convertInput");
				return parallelWorker.importData(tempFileForWorkingDataSet);
			}
			throw new IllegalArgumentException("cannot convert from " + input
				.getClass());
		}

		private Object convert2Local(Object input) {
			if (suffixOfImportedFile != null) {
				Path result = Routines.supplyWithExceptionHandling(() -> Files
					.createTempFile("", suffixOfImportedFile), log, "output conversion");
				parallelWorker.exportData(input, result);
				parallelWorker.deleteData(input);
				return result;
			}
			else if (workingDataSet != null && tempFileForWorkingDataSet != null) {
				parallelWorker.exportData(input, tempFileForWorkingDataSet);
				Dataset tempDataset = (Dataset) Routines.supplyWithExceptionHandling(
					() -> ioService.open(tempFileForWorkingDataSet.toString()), log,
					"convertOutput");
				tempDataset.copyInto(workingDataSet);
				Routines.runWithExceptionHandling(() -> Files.delete(
					tempFileForWorkingDataSet), log, "");
				return workingDataSet;
			}
			throw new IllegalArgumentException("bad arguments");
		}

}
