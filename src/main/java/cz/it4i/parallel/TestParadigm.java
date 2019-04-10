package cz.it4i.parallel;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigmProfile;

import cz.it4i.parallel.ImageJServerParadigm.Host;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TestParadigm implements ParallelizationParadigm
{
	private final AbstractImageJServerRunner runner;
	private final ParallelizationParadigm paradigm;
	private boolean closed = false;

	public TestParadigm( AbstractImageJServerRunner runner, Context context )
	{
		this.paradigm = initParadigm( runner, context );
		this.runner = runner;
	}

	public static ParallelizationParadigm localImageJServer( String fiji, Context context ) {
		return new TestParadigm( new ImageJServerRunner( fiji ), context );
	}

	public static ParallelizationParadigm localImageJServer(Context context) {
		return new TestParadigm(new EmbeddedImageJServerRunner(), context);
	}

	private static ParallelizationParadigm initParadigm( AbstractImageJServerRunner runner, Context context )
	{
		runner.start();
		int nCores = runner.getNCores();
		List<Host> hosts = runner.getPorts().stream()
			.map(port -> new ImageJServerParadigm.Host("localhost:" + port, nCores))
				.collect( Collectors.toList() );
		return configureParadigm( context.service( ParallelService.class ), hosts );
	}

	private static ParallelizationParadigm configureParadigm(
		ParallelService parallelService, List<Host> hosts)
	{
		parallelService.deleteProfiles();
		parallelService.addProfile(new ParallelizationParadigmProfile(
				ImageJServerParadigm.class, "lonelyBiologist01"));
		parallelService.selectProfile("lonelyBiologist01");

		ParallelizationParadigm paradigm = parallelService.getParadigm();
		((ImageJServerParadigm) paradigm).setHosts(hosts);
		paradigm.init();
		return paradigm;
	}

	@Override
	public void init()
	{
		checkClosed();
		paradigm.init();
	}

	@Override
	public List< Map< String, Object > > runAll( Class< ? extends Command > commandClazz, List< Map< String, Object > > parameters )
	{
		checkClosed();
		return paradigm.runAll( commandClazz, parameters );
	}

	@Override
	public List< CompletableFuture< Map< String, Object > > > runAllAsync( Class< ? extends Command > commandClazz, List< Map< String, Object > > parameters )
	{
		checkClosed();
		return paradigm.runAllAsync( commandClazz, parameters );
	}

	@Override
	public List< Map< String, Object > > runAll( String s, List< Map< String, Object > > list )
	{
		checkClosed();
		return paradigm.runAll( s, list );
	}

	@Override
	public List< CompletableFuture< Map< String, Object > > > runAllAsync( String s, List< Map< String, Object > > list )
	{
		checkClosed();
		return paradigm.runAllAsync( s, list );
	}

	@Override
	public void close()
	{
		closed = true;
		paradigm.close();
		runner.close();
	}

	private void checkClosed()
	{
		if(closed)
			throw new RuntimeException( "ParallelizationParadigm is used after it has been closed." );
	}

}
