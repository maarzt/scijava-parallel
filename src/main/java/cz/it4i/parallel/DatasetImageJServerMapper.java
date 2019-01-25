
package cz.it4i.parallel;

import static cz.it4i.parallel.Routines.getSuffix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import net.imagej.Dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetImageJServerMapper extends AbstractMapper {

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.DatasetImageJServerMapper.class);

	private String suffixOfImportedFile;

	private Dataset workingDataSet;

	private Path tempFileForWorkingDataSet;

	public DatasetImageJServerMapper() {
		super(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			ImageJServerParadigm.class))), Collections.unmodifiableSet(new HashSet<>(
				Arrays.asList("net.imagej.Dataset"))));
	}

	@Override
	public Object map2Paradigm(Object input) {
		if (input instanceof Path) {
			Path path = (Path) input;
			String filename = path.getFileName().toString();
			suffixOfImportedFile = getSuffix(filename);
			return pw.importData(path);
		}
		else if (input instanceof Dataset) {
			workingDataSet = (Dataset) input;
			String workingSuffix = getSuffix(workingDataSet.getName());
			tempFileForWorkingDataSet = Routines.supplyWithExceptionHandling(
				() -> Files.createTempFile("", workingSuffix), log, "convertInput");
			Routines.runWithExceptionHandling(() -> ioService.save(input,
				tempFileForWorkingDataSet.toString()), log, "convertInput");
			return pw.importData(tempFileForWorkingDataSet);
		}
		throw new IllegalArgumentException("cannot convert from " + input
			.getClass());
	}

	@Override
	public Object map2Local(Object input) {
		// TODO Auto-generated method stub
		return null;
	}

}
