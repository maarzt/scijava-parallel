
package test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.ui.RunImageJServerOnHPCCommand;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>DemonstrateOstravaParadigm")
public class ScriptEvalRemotelyOnHPC extends AbstractBaseDemonstrationExample {

	private static final Logger log = LoggerFactory.getLogger(
		ScriptEvalRemotelyOnHPC.class);

	@Parameter
	private int step = 30;

	@Parameter
	private CommandService commandService;

	public static void main(final String... args) {

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(ScriptEvalRemotelyOnHPC.class, true);
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

	@Override
	protected AbstractImageJServerRunner constructImageJServerRunner() {
		try {
			Map<String, Object> result = commandService.run(
				RunImageJServerOnHPCCommand.class, true).get().getOutputs();
			return (AbstractImageJServerRunner) result.get("runner");
		}
		catch (InterruptedException | ExecutionException exc) {
			throw new RuntimeException(exc);
		}

	}
}
