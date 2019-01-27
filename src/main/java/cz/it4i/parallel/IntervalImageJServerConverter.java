
package cz.it4i.parallel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.imglib2.Interval;

import org.scijava.plugin.Plugin;

@Plugin(type = ParallelizationParadigmConverter.class)
public class IntervalImageJServerConverter extends
	AbstractParallelizationParadigmConverter<Interval>
{

	private Interval workingInterval;

	public IntervalImageJServerConverter() {
		super(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			ImageJServerParadigm.class))), Interval.class);
	}

	@Override
	public <T> T convert(Object src, Class<T> dest) {
		if (dest == Object.class) {
			workingInterval = (Interval) src;
			return Routines.castTo("{min:[" + getAsString(workingInterval,
				i -> workingInterval.min(i)) + "], max: [" + getAsString(
					workingInterval, i -> workingInterval.max(i)) + "] }");
		}
		else if (dest == Interval.class) {
			return Routines.<T> castTo(src);
		}

		throw new IllegalArgumentException("cannot convert from " + src.getClass());
	}

	private String getAsString(Interval interval,
		Function<Integer, Long> action)
	{
		return IntStream.range(0, interval.numDimensions()).mapToObj(i -> "" +
			action.apply(i)).collect(Collectors.joining(", "));
	}

}
