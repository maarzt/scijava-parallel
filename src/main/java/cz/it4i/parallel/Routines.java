
package cz.it4i.parallel;

public class Routines {

	public interface RunnableWithException {

		public void run() throws Exception;
	}

	public interface SupplierWithException<T> {

		public T supply() throws Exception;
	}

	public static void runWithExceptionHandling(RunnableWithException runnable)
	{
		try {
			runnable.run();
		}
		catch (Exception exc) {
			throw new RuntimeException(exc);
		}
	}

	public static <T> T supplyWithExceptionHandling(
		SupplierWithException<T> supplier)
	{
		try {
			return supplier.supply();
		}
		catch (Exception exc) {
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
