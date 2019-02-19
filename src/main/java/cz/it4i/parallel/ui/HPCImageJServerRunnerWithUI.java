
package cz.it4i.parallel.ui;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.scijava.Context;

import cz.it4i.parallel.HPCImageJServerRunner;
import cz.it4i.parallel.HPCSettings;

public class HPCImageJServerRunnerWithUI extends HPCImageJServerRunner {

	private JDialog dialog;
	private JLabel label;

	public HPCImageJServerRunnerWithUI(HPCSettings settings) {
		super(settings);
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

		imageJServerStarted();
		super.start();
		imageJServerRunning();
	}

	private void imageJServerStarted() {
		dialog.setVisible(false);
		this.label.setText("Waiting for a ImageJ server start.");
		dialog.setVisible(true);
	}

	private void imageJServerRunning() {
		dialog.setVisible(false);
	}

	@Override
	public void close() {
		label.setText("Waiting for stop.");
		dialog.setVisible(true);
		super.close();
		dialog.setVisible(false);
	}

	public static HPCImageJServerRunner gui(Context context) {
		return new HPCImageJServerRunnerWithUI(HPCSettingsGui.showDialog(context));
	}
}
