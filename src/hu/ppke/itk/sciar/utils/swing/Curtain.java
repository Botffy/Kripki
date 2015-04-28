package hu.ppke.itk.sciar.utils.swing;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;

public class Curtain extends JPanel {
	public Curtain() {
		setLayout(new BorderLayout());
		JProgressBar spinner = new JProgressBar();
		spinner.setIndeterminate(true);
		add(spinner, BorderLayout.CENTER);
	}
}
