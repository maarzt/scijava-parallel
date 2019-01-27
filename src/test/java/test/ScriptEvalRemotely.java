
package test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>DemonstrateOstravaParadigm")
public class ScriptEvalRemotely extends AbstractBaseDemonstrationExample {

	private static final Logger log = LoggerFactory.getLogger(AddDoubles.class);

	protected static int step = 30;

	public static void main(final String... args) {

		final ImageJ ij = new ImageJ();
		ij.command().run(ScriptEvalRemotely.class, true);
	}

	@Override
	protected void callRemotePlugin(ParallelizationParadigm paradigm) {
		List<Map<String, Object>> paramsList = new LinkedList<>();
		for (int i = 0; i < step; i++) {
			Map<String, Object> params = new HashMap<>();
			params.put("language", "ij1");
			params.put("script", "print('hello from script" + i +
				"'); getDirectory('home'); exec('whoami');");
			paramsList.add(params);
		}
		List<Map<String, Object>> result = paradigm.runAll(
			"net.imagej.server.external.ScriptEval", paramsList);
		log.info("result: " + result);

	}
}
