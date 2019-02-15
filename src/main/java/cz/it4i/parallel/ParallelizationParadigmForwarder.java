package cz.it4i.parallel;

import org.scijava.command.Command;
import org.scijava.parallel.ParallelizationParadigm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ParallelizationParadigmForwarder implements ParallelizationParadigm
{
	private final ParallelizationParadigm paradigm;

	public ParallelizationParadigmForwarder( ParallelizationParadigm paradigm )
	{
		this.paradigm = paradigm;
	}

	@Override
	public void init()
	{
		paradigm.init();
	}

	@Override
	public List< Map< String, Object > > runAll( Class< ? extends Command > commandClazz, List< Map< String, Object > > parameters )
	{
		return paradigm.runAll( commandClazz, parameters );
	}

	@Override
	public List< CompletableFuture< Map< String, Object > > > runAllAsync( Class< ? extends Command > commandClazz, List< Map< String, Object > > parameters )
	{
		return paradigm.runAllAsync( commandClazz, parameters );
	}

	@Override
	public List< Map< String, Object > > runAll( String s, List< Map< String, Object > > list )
	{
		return paradigm.runAll( s, list );
	}

	@Override
	public List< CompletableFuture< Map< String, Object > > > runAllAsync( String s, List< Map< String, Object > > list )
	{
		return paradigm.runAllAsync( s, list );
	}

	@Override
	public void close()
	{
		paradigm.close();
	}
}
