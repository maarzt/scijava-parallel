
package test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imagej.ImageJ;

import org.scijava.parallel.ParallelizationParadigm;

import cz.it4i.parallel.TestParadigm;

public class ScriptEvalRemotely {

	public static void main(final String... args) {

		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		try( ParallelizationParadigm paradigm = TestParadigm.localImageJServer( Config.getFijiExecutable(), ij.context() )) {
			List<Map<String, Object>> paramsList = new LinkedList<>();
			for (int i = 0; i < 10; i++) {
				Map<String, Object> params = new HashMap<>();
				params.put("language", "ij1");
				params.put("script", "print('hello from script" + i +
						"'); getDirectory('home'); exec('whoami');");
				paramsList.add(params);
			}
			paradigm.runAll("net.imagej.server.external.ScriptEval", paramsList);
		}
	}
}
