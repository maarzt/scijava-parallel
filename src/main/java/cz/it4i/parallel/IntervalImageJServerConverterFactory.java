
package cz.it4i.parallel;

import static cz.it4i.parallel.Routines.castTo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import net.imagej.Dataset;
import net.imagej.Extents;
import net.imagej.server.mixins.Mixins.ObjectMapperModificator;
import net.imagej.server.mixins.Mixins.SerializerModificator;
import net.imglib2.Interval;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = ParallelizationParadigmConverterFactory.class)
public class IntervalImageJServerConverterFactory extends
	AbstractParallelizationParadigmConverterFactory<Interval>
{

	private final static Logger log = LoggerFactory.getLogger(
		cz.it4i.parallel.IntervalImageJServerConverterFactory.class);

	public IntervalImageJServerConverterFactory() {
		super(Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			ImageJServerParadigm.class))), Interval.class);
	}

	@Override
	public Converter<Object, Interval> createConverterForWorker(Object worker) {
		return new P_Converter();
	}

	@Plugin(type = Converter.class)
	static public class IntervalConverter extends
		org.scijava.convert.AbstractConverter<String, Interval>
	{

		@Override
		public boolean canConvert(Object src, Class<?> dest) {
			return canConvert(src, (Type) dest);
		}

		@Override
		public boolean canConvert(Object src, Type dest) {
			return super.canConvert(src, dest) && canConvert(src);
		}

		@Override
		public <T> T convert(Object src, Class<T> dest) {
			JSONObject json = new JSONObject(src.toString());
			long[] min = ((JSONArray) json.get("min")).toList().stream().map(
				obj -> ((Number) obj).longValue()).mapToLong(l -> l.longValue())
				.toArray();
			long[] max = ((JSONArray) json.get("max")).toList().stream().map(
				obj -> ((Number) obj).longValue()).mapToLong(l -> l.longValue())
				.toArray();

			return castTo(new Extents(min, max));

		}

		@Override
		public Class<Interval> getOutputType() {
			return Interval.class;
		}

		@Override
		public Class<String> getInputType() {
			return String.class;
		}

		private boolean canConvert(Object src) {
			if (src instanceof String) {
				try {

					String str = (String) src;
					new JSONObject(str);
					return true;
				}
				catch (JSONException e) {
					return false;
				}
			}
			return false;
		}

	}

	@Plugin(type = ObjectMapperModificator.class)
	public static class IntervalSerializerModificator extends
		SerializerModificator<Interval>
	{

		public IntervalSerializerModificator() {
			super(Arrays.asList(Interval.class), Arrays.asList(Dataset.class),
				new P_Serializer(), Interval.class);
		}

	}

	private class P_Converter extends AbstractConverter {

		private IntervalConverter delegateConverter = new IntervalConverter();

		private ObjectMapper mapper;

		public P_Converter() {
			mapper = new ObjectMapper();
			new IntervalSerializerModificator().accept(mapper);
		}

		@Override
		public <T> T convert(Object src, Class<T> dest) {
			if (src.getClass() == String.class && dest == Interval.class) {
				return delegateConverter.convert(src, dest);
			}
			if (Interval.class.isAssignableFrom(src.getClass())) {
				if (dest == Object.class) {
					return Routines.castTo(Routines.supplyWithExceptionHandling(
						() -> mapper.writeValueAsString(src), log, ""));
				}
				else if (dest == Interval.class) {
					return Routines.<T> castTo(src);
				}
			}
			throw new IllegalArgumentException("cannot convert from " + src + " to " +
				dest);
		}

	}

	private static class P_Serializer extends StdSerializer<Interval> {

		protected P_Serializer(Class<Interval> t) {
			super(t);
		}

		public P_Serializer() {
			this(null);
		}

		@Override
		public void serialize(Interval value, JsonGenerator gen,
			SerializerProvider provider) throws IOException
		{

			gen.writeStartObject();
			gen.writeArrayFieldStart("min");
			for (int i = 0; i < value.numDimensions(); i++) {
				gen.writeNumber(value.min(i));
			}
			gen.writeEndArray();
			gen.writeArrayFieldStart("max");
			for (int i = 0; i < value.numDimensions(); i++) {
				gen.writeNumber(value.max(i));
			}
			gen.writeEndArray();
			gen.writeEndObject();
		}

	}

}
