
package cz.it4i.parallel;

import org.slf4j.Logger;

public class Routines {

	public interface RunnableWithException {

		public void run() throws Exception;
	}

	public interface SupplierWithException<T> {

		public T supply() throws Exception;
	}

	public static void runWithExceptionHandling(RunnableWithException runnable,
		Logger log, String action)
	{
		try {
			runnable.run();
		}
		catch (Exception exc) {
			log.error(action, exc);
			throw new RuntimeException(exc);
		}
	}

	public static <T> T supplyWithExceptionHandling(
		SupplierWithException<T> supplier, Logger log, String action)
	{
		try {
			return supplier.supply();
		}
		catch (Exception exc) {
			log.error(action, exc);
			throw new RuntimeException(exc);
		}
	}

	public static String getSuffix(String filename) {
		return filename.substring(filename.lastIndexOf('.'), filename.length());
	}

	public static <T> T castTo(Object src) {
		@SuppressWarnings("unchecked")
		T result = (T) src;
		return result;
	}
}
