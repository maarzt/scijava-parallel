
package cz.it4i.parallel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.imglib2.Interval;

import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = ParallelizationParadigmParameterMapper.class)
public class IntervalImageJServerMapper extends AbstractMapper {

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.IntervalImageJServerMapper.class);

	private Interval workingInterval;

	public IntervalImageJServerMapper() {
		super(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			ImageJServerParadigm.class))), Collections.unmodifiableSet(new HashSet<>(
				Arrays.asList("net.imglib2.Interval"))));
	}

	@Override
	public Object map2Paradigm(Object input) {
		if (input instanceof Interval) {
			workingInterval = (Interval) input;
			return "{min:[" + getAsString(workingInterval, i -> workingInterval.min(
				i)) + "], max: [" + getAsString(workingInterval, i -> workingInterval
					.max(i)) + "] }";
		}

		throw new IllegalArgumentException("cannot convert from " + input
			.getClass());
	}

	@Override
	public Object map2Local(Object input) {
		return workingInterval;
	}

	private String getAsString(Interval interval,
		Function<Integer, Long> action)
	{
		return IntStream.range(0, interval.numDimensions()).mapToObj(i -> "" +
			action.apply(i)).collect(Collectors.joining(", "));
	}
}
