
package cz.it4i.parallel.ui;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import cz.it4i.parallel.HPCImageJServerRunner;

class HPCImageJServerRunnerWithUI extends HPCImageJServerRunner {

	private JDialog dialog;
	private JLabel label;

	public HPCImageJServerRunnerWithUI(String host, String userName, File keyFile,
		String keyFilePassword, String remoteDirectory, String command, int nodes,
		int ncpus)
	{
		super(host, userName, keyFile, keyFilePassword, remoteDirectory, command,
			nodes, ncpus);
	}

	@Override
	public void start() {
		this.dialog = new JOptionPane().createDialog("Waiting");
		JPanel panel = new JPanel(new BorderLayout());
		dialog.setContentPane(panel);
		this.label = new JLabel("Waiting for job schedule.");
		label.setHorizontalAlignment(SwingConstants.CENTER);

		panel.add(label, BorderLayout.CENTER);
		dialog.setModal(false);
		dialog.setVisible(true);

		super.start();
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
