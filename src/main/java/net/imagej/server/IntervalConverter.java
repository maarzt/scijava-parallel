
package net.imagej.server;

import static cz.it4i.parallel.Routines.castTo;

import java.lang.reflect.Type;

import net.imagej.Extents;
import net.imglib2.Interval;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

@Plugin(type = Converter.class)
public class IntervalConverter extends AbstractConverter<String, Interval> {

	@Override
	public boolean canConvert(Object src, Class<?> dest) {
		return super.canConvert(src, dest) && checkFormat(src);
	}

	@Override
	public boolean canConvert(Object src, Type dest) {
		return super.canConvert(src, dest) && checkFormat(src);
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

	private boolean checkFormat(Object src) {
		try {
			new JSONObject(src.toString());
		}
		catch (JSONException e) {
			return false;
		}
		return true;
	}

}
