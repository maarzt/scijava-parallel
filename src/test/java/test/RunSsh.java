
package test;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;
import org.scijava.widget.TextWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.parallel.ClusterJobLauncher;
import cz.it4i.parallel.ClusterJobLauncher.Job;
import cz.it4i.parallel.Routines;

@Plugin(type = Command.class, headless = false, menuPath = "Plugins>RunSsh")
public class RunSsh implements Command {

	private final static Logger log = LoggerFactory.getLogger(RunSsh.class);

	public static void main(String[] args) {
		// Launch ImageJ as usual
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(RunSsh.class, true);
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

	@Parameter
	private UIService uiService;

	@Override
	public void run() {
		Routines.runWithExceptionHandling(() -> {
			try (ClusterJobLauncher launcher = new ClusterJobLauncher(host, userName,
				keyFile.toString(), keyFilePassword))
			{
				Job job = launcher.submit(remoteDirectory, command,
					"-Dimagej.legacy.modernOnlyCommands=true -- --ij2 --headless --server",
					nodes, ncpus);
				JDialog dialog = new JOptionPane().createDialog("Waiting");
				dialog.getContentPane().removeAll();
				JPanel panel = new JPanel(new BorderLayout());
				dialog.getContentPane().add(panel);
				JLabel label = new JLabel("Waiting for job schedule");

				panel.add(label, BorderLayout.CENTER);
				dialog.setModal(false);
				dialog.pack();
				dialog.setVisible(true);
				job.waitForRunning();
				List<Integer> ports = job.createTunnels(8080, 8080);
				boolean running;
				do {
					running = checkImageJServerRunning(ports.get(0));
					Thread.sleep(1000);
				}
				while (!running);
				dialog.setVisible(false);

				uiService.showDialog("Tunnels opened on " + ports +
					". Confirm to close");
				label.setText("Waiting for stop");
				dialog.pack();
				dialog.setVisible(true);
				job.stop();
				dialog.setVisible(false);
				System.exit(0);
			}
		}, log, "");

	}

	private boolean checkImageJServerRunning(int port) {
		boolean running = true;
		try {
			if (checkModulesURL(port) != 200) {
				throw new IllegalStateException(
					"Different server than ImageJServer is running on localhost:" + port);
			}
		}
		catch (SocketException exc) {
			running = false;
		}
		catch (IOException exc) {
			log.error("connect ot ImageJServer", exc);
			throw new RuntimeException(exc);
		}
		return running;
	}

	private int checkModulesURL(int port) throws IOException,
		MalformedURLException, ProtocolException
	{
		HttpURLConnection hc;
		hc = (HttpURLConnection) new URL("http://localhost:" + port + "/modules")
			.openConnection();
		hc.setRequestMethod("GET");
		hc.connect();
		hc.disconnect();
		return hc.getResponseCode();
	}

}
