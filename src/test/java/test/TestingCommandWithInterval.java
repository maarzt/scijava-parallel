
package test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imagej.Extents;
import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;

import cz.it4i.parallel.TestParadigm;

@Plugin(type = Command.class, headless = true)
public class TestingCommandWithInterval
{
	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		try( ParallelizationParadigm paradigm = TestParadigm.localImageJServer( Config.getFijiExecutable(), ij.context() )) {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("a", 11);
			parameters.put("interval", new Extents(new long[] { 10 }, new long[] {
				10 }));

			List<Map<String, Object>> results = paradigm.runAll(
					"net.imagej.server.TestingCommand", Collections.singletonList(parameters));
			System.out.println("result: " + results);
		}
	}

}
