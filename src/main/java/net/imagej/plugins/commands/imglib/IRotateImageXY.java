package net.imagej.plugins.commands.imglib;

import java.nio.file.Path;

import org.scijava.command.Command;

//TODO should convert argument of different type
public interface IRotateImageXY extends Command{

	Path getDataset();
	
	void setDataset(Path file);
	
	void setAngle(String angle);
	
	
}
