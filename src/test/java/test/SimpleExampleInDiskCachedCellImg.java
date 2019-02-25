package test;

import io.scif.services.DatasetIOService;

import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.scijava.Context;
import org.scijava.parallel.ParallelizationParadigm;

import bdv.util.BdvFunctions;
import cz.it4i.parallel.TestParadigm;

public class SimpleExampleInDiskCachedCellImg
{

	public static void main(String... args) 
	{

		Context context = new Context();
		DatasetIOService ioService = context.service( DatasetIOService.class );
		ParallelizationParadigm paradigm = TestParadigm.localImageJServer(Config
			.getFijiExecutable(), context);

		final CellLoader<UnsignedByteType> loader = cell -> {
			RotateSingleDataset.rotateSingleDataset(ioService, paradigm); // failing
			// ScriptEvalRemotely.run(paradigm); // working
		};
		DiskCachedCellImg<UnsignedByteType, ?> img = createCellImage(loader);
		BdvFunctions.show(img, "title").setDisplayRange(0, 1);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> paradigm.close()));
	}

	private static DiskCachedCellImg< UnsignedByteType, ? > createCellImage( CellLoader< UnsignedByteType > loader )
	{
		long[] imageSize = { 100, 100, 100 };
		int[] cellSize = { 50, 50, 50};
		final DiskCachedCellImgOptions options = DiskCachedCellImgOptions.options().cellDimensions( cellSize );
		final DiskCachedCellImgFactory< UnsignedByteType > factory = new DiskCachedCellImgFactory<>( new UnsignedByteType(), options );
		return factory.create( imageSize, loader );
	}
}
