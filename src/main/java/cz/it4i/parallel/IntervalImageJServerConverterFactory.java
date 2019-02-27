
package cz.it4i.parallel;

import static cz.it4i.parallel.Routines.castTo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import net.imagej.Dataset;
import net.imagej.server.json.SciJavaJsonSerializer;
import net.imglib2.FinalInterval;
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
			JSONObject json = src instanceof JSONObject ? (JSONObject) src
				: new JSONObject(src.toString());
			long[] min = ((JSONArray) json.get("min")).toList().stream().map(
				obj -> ((Number) obj).longValue()).mapToLong(l -> l.longValue())
				.toArray();
			long[] max = ((JSONArray) json.get("max")).toList().stream().map(
				obj -> ((Number) obj).longValue()).mapToLong(l -> l.longValue())
				.toArray();

			return castTo(new FinalInterval(min, max));

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

	@Plugin(type = SciJavaJsonSerializer.class)
	public static class IntervalJSONSerializer implements
		SciJavaJsonSerializer<Interval>
	{

		@Override
		public boolean isSupportedBy(Class<?> desiredClass) {
			return SciJavaJsonSerializer.super.isSupportedBy(desiredClass) &&
				!Dataset.class.isAssignableFrom(desiredClass);
		}

		@Override
		public void serialize(Interval interval, JsonGenerator gen,
			SerializerProvider serializers) throws IOException
		{

			gen.writeStartObject();
			gen.writeArrayFieldStart("min");
			for (int i = 0; i < interval.numDimensions(); i++) {
				gen.writeNumber(interval.min(i));
			}
			gen.writeEndArray();
			gen.writeArrayFieldStart("max");
			for (int i = 0; i < interval.numDimensions(); i++) {
				gen.writeNumber(interval.max(i));
			}
			gen.writeEndArray();
			gen.writeEndObject();

		}


		@Override
		public Class<Interval> handleType() {
			return Interval.class;
		}

	}

	private class P_Converter extends AbstractConverter {

		private IntervalConverter delegateConverter = new IntervalConverter();

		private ObjectMapper mapper;

		public P_Converter() {
			mapper = new ObjectMapper();
			IntervalJSONSerializer serializer = new IntervalJSONSerializer();
			serializer.register(mapper);
		}

		@Override
		public <T> T convert(Object src, Class<T> dest) {
			if ((src.getClass() == JSONObject.class || src
				.getClass() == String.class) && dest == Interval.class)
			{
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



}
