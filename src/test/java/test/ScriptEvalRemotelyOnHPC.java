
package test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cz.it4i.parallel.HPCImageJServerRunner;
import cz.it4i.parallel.TestParadigm;
import cz.it4i.parallel.ui.HPCImageJServerRunnerWithUI;
import net.imagej.ImageJ;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>DemonstrateOstravaParadigm")
public class ScriptEvalRemotelyOnHPC implements Command
{

	private static final Logger log = LoggerFactory.getLogger(
		ScriptEvalRemotelyOnHPC.class);

	@Parameter
	private int count = 30;

	@Parameter
	private Context context;

	public static void main(final String... args) {

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(ScriptEvalRemotelyOnHPC.class, true);
	}

	@Override
	public void run()
	{
		HPCImageJServerRunner runner = HPCImageJServerRunnerWithUI.gui( context );
		try( ParallelizationParadigm paradigm = new TestParadigm( runner, context ) ) {
			List<Map<String, Object>> paramsList = new LinkedList<>();
			for ( int i = 0; i < count; i++) {
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
}
