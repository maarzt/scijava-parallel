package test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigmProfile;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.ImageJServerParadigm;

@Plugin(type = Command.class, headless = true,
	menuPath = "Plugins>DemonstrateOstravaParadigm")
public class DemonstrationParadigmScriptEval implements Command {

	private static int step = 30;

	private static List<String> hosts = Arrays.asList("localhost:8080");
	
	
	public static final Logger log = LoggerFactory.getLogger(
		DemonstrationParadigmScriptEval.class);

	@Parameter
	private ParallelService parallelService;

	public static void main(final String... args) {
		final ImageJ ij = new ImageJ();
		ij.command().run(DemonstrationParadigmScriptEval.class, true);
	}

	@Override
	public void run() {
		try(ImageJServerRunner imageJServerRunner = new ImageJServerRunner()) {
			try( ParallelizationParadigm paradigm = configureParadigm() ) {
				doTest(paradigm);
			}
			
		}
	}

	private void doTest(final ParallelizationParadigm paradigm)
	{
		List<String> commands = new LinkedList<>();
		List<Map<String,Object>> paramsList = new LinkedList<>();
		for (int i = 0; i < step; i++) {
			commands.add("net.imagej.server.external.ScriptEval");
			Map<String, Object> params = new HashMap<>();
			params.put("language", "ij1");
			params.put("script", "print('hello from script" + i + "'); getDirectory('home'); exec('whoami');");
			paramsList.add(params);
		}
		List<Map<String,Object>> result = paradigm.runAllCommands(commands, paramsList);
		log.info("result: " + result);
		
	}
	
	private ParallelizationParadigm configureParadigm() {
		parallelService.deleteProfiles();
		parallelService.addProfile(new ParallelizationParadigmProfile(
			ImageJServerParadigm.class, "lonelyBiologist01"));
		parallelService.selectProfile("lonelyBiologist01");
	
		ParallelizationParadigm paradigm = parallelService.getParadigm();
		((ImageJServerParadigm) paradigm).setHosts(hosts);
		paradigm.init();
		return paradigm;
	}
}