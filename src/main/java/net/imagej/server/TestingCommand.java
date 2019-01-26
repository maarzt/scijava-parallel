
package net.imagej.server;

import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imglib2.Interval;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Op.class)
public class TestingCommand extends AbstractOp {

	@Parameter
	private Interval interval;

	@Parameter
	private Integer a;

	@Parameter(type = ItemIO.OUTPUT)
	private Integer b;

	@Override
	public void run() {
		b = a * a;
	}

}
