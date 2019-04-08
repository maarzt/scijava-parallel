package cz.it4i.parallel.plugins;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import cz.it4i.parallel.Routines;
import lombok.AllArgsConstructor;

@Plugin(headless = true, type = ThreadRunner.class)
public class ThreadRunner implements Command {

	@Parameter
	private ThreadService service;

	@Parameter
	private Context context;

	@Parameter
	private ModuleService moduleService;

	@Parameter(type = ItemIO.INPUT)
	private List<Map<String, Object>> inputs;

	@Parameter(type = ItemIO.INPUT)
	private String moduleId;

	@Parameter(type = ItemIO.OUTPUT)
	private List<Map<String, Object>> outputs;

	@Override
	public void run()
	{
		final Executor executor = new ThreadService2ExecutorAdapter(service);
		final ModuleInfo info = moduleService.getModuleById(moduleId);
		outputs = new LinkedList<>();
		inputs.stream().map(input -> CompletableFuture.supplyAsync(() -> Routines
			.supplyWithExceptionHandling(() -> moduleService.run(info, true, input)
				.get()).getOutputs(), executor)).collect(Collectors.toList()).stream()
			.map(CompletableFuture::join).collect(Collectors.toList());
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
