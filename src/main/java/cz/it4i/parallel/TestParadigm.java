package cz.it4i.parallel;

import org.scijava.Context;
import org.scijava.parallel.ParallelService;
import org.scijava.parallel.ParallelizationParadigm;
import org.scijava.parallel.ParallelizationParadigmProfile;

import java.util.List;
import java.util.stream.Collectors;

public class TestParadigm extends ParallelizationParadigmForwarder
{
	private final AbstractImageJServerRunner runner;

	public TestParadigm( AbstractImageJServerRunner runner, Context context )
	{
		super( initParadigm( runner, context ) );
		this.runner = runner;
	}

	public static ParallelizationParadigm localImageJServer( String fiji, Context context ) {
		return new TestParadigm( new ImageJServerRunner( fiji ), context );
	}

	private static ParallelizationParadigm initParadigm( AbstractImageJServerRunner runner, Context context )
	{
		runner.start();
		List< String > hosts = runner.getPorts().stream()
				.map( port -> "localhost:" + port )
				.collect( Collectors.toList() );
		return configureParadigm( context.service( ParallelService.class ), hosts );
	}

	@Override
	public void close()
	{
		super.close();
		runner.close();
	}

	private static ParallelizationParadigm configureParadigm( ParallelService parallelService, List<String> hosts ) {
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
