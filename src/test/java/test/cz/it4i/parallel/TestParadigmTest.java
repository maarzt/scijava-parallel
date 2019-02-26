package test.cz.it4i.parallel;

import cz.it4i.parallel.TestParadigm;
import org.junit.Ignore;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;
import test.Config;

import java.util.Collections;

/**
 * Tests {@link TestParadigm}.
 *
 * @autor Matthias Arzt
 */
public class TestParadigmTest
{
	@Ignore("Test depends on correctly set up ImageJ Server.")
	@Test(expected = RuntimeException.class)
	public void testExceptionAfterClose() {
		ParallelizationParadigm paradigm = TestParadigm.localImageJServer( Config.getFijiExecutable(), new Context() );
		paradigm.close();
		paradigm.runAll("net.imagej.server.external.ScriptEval", Collections.emptyList());
	}
}
