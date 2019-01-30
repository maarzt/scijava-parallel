
package test;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;
import org.scijava.widget.TextWidget;

import cz.it4i.parallel.AbstractImageJServerRunner;
import cz.it4i.parallel.HPCImageJServerRunner;

@Plugin(type = Command.class, headless = false)
public class RotateFileOnHPC extends RotateFile {

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(RotateFileOnHPC.class, true);
	}

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Host name")
	private String host;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "User name")
	private String userName;

	@Parameter(style = FileWidget.OPEN_STYLE, label = "Key file")
	private File keyFile;

	@Parameter(style = TextWidget.PASSWORD_STYLE, label = "Key file password",
		persist = false)
	private String keyFilePassword;

	// for salomon /scratch/work/project/dd-18-42/apps/fiji-with-server
	@Parameter(style = TextWidget.FIELD_STYLE,
		label = "Remote directory with Fiji")
	private String remoteDirectory;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Remote ImageJ command")
	private String command = "ImageJ-linux64";

	// for salomon run-workers.sh
	@Parameter(style = TextWidget.FIELD_STYLE, label = "Number of nodes")
	private int nodes;

	@Parameter(style = TextWidget.FIELD_STYLE, label = "Number of cpus per node")
	private int ncpus;

	@Override
	protected AbstractImageJServerRunner constructImageJServerRunner() {
		return new P_Runner(host, userName, keyFile, keyFilePassword,
			remoteDirectory, command, nodes, ncpus);
	}

	private static class P_Runner extends HPCImageJServerRunner {

		private JDialog dialog;
		private JLabel label;

		public P_Runner(String host, String userName, File keyFile,
			String keyFilePassword, String remoteDirectory, String command, int nodes,
			int ncpus)
		{
			super(host, userName, keyFile, keyFilePassword, remoteDirectory, command,
				nodes, ncpus);
		}

		@Override
		public AbstractImageJServerRunner startIfNecessary() {
			this.dialog = new JOptionPane().createDialog("Waiting");
			JPanel panel = new JPanel(new BorderLayout());
			dialog.setContentPane(panel);
			this.label = new JLabel("Waiting for job schedule.");
			label.setHorizontalAlignment(SwingConstants.CENTER);

			panel.add(label, BorderLayout.CENTER);
			dialog.setModal(false);
			dialog.setVisible(true);

			return super.startIfNecessary();
		}

		@Override
		protected void imageJServerStarted() {
			dialog.setVisible(false);
			this.label.setText("Waiting for a ImageJ server start.");
			dialog.setVisible(true);
		}

		@Override
		protected void imageJServerRunning() {
			dialog.setVisible(false);
		}

		@Override
		public void close() {
			label.setText("Waiting for stop.");
			dialog.setVisible(true);
			super.close();
			dialog.setVisible(false);
		}
	}

}
