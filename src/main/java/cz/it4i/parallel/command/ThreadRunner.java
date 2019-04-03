package cz.it4i.parallel.command;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.scijava.Context;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import cz.it4i.parallel.Routines;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Plugin(headless = true, type = ThreadRunner.class)
public class ThreadRunner implements ParallelizationParadigm {

	@Parameter
	private ThreadService service;

	@Parameter
	private Context context;

	@Setter
	private Map<String, ModuleInfo> modules;

	@Parameter
	private ModuleService moduleService;

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Map<String, Object>> runAll(String commandName,
		List<Map<String, Object>> parameters)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List<CompletableFuture<Map<String, Object>>> runAllAsync(
		String commandName, List<Map<String, Object>> parameters)
	{
		Executor executor = new ThreadService2ExecutorAdapter(service);
		final ModuleInfo info = modules.get(commandName);
		List<CompletableFuture<Map<String, Object>>> result = new LinkedList<>();
		for (Map<String, Object> input : parameters) {
			service.registerEventHandlers();
			result.add(CompletableFuture.supplyAsync(() -> Routines
				.supplyWithExceptionHandling(
				() -> moduleService.run(info, true, input).get()).getOutputs(),
				executor));
		}

		return result;
	}

	@AllArgsConstructor
	private static class ThreadService2ExecutorAdapter implements Executor {

		private ThreadService service;

		@Override
		public void execute(Runnable command) {
			service.run(command);
		}

	}


}
