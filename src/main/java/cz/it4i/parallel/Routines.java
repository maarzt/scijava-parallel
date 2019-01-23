package cz.it4i.parallel;

import org.slf4j.Logger;

public class Routines {
	
	public interface RunnableWithException {
		public void run() throws Exception;
	}
	
	public interface SupplierWithException<T> {
		public T supply() throws Exception;
	}
	
	public static void runWithExceptionHandling(RunnableWithException runnable, Logger log, String action) {
		try {
			runnable.run();
		}
		catch (Exception exc) {
			log.error(action, exc);
			throw new RuntimeException(exc);
		}
	}
	
	public static<T> T supplyWithExceptionHandling(SupplierWithException<T> supplier, Logger log, String action) {
		try {
			return supplier.supply();
		}
		catch (Exception exc) {
			log.error(action, exc);
			throw new RuntimeException(exc);
		}
	}
}
