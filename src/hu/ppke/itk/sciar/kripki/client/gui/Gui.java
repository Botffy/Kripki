package hu.ppke.itk.sciar.kripki.client.gui;

import javax.swing.SwingUtilities;
import net.infotrek.util.prefs.FilePreferencesFactory;


public class Gui implements Runnable {
	public static void main(String[] args) {
		System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());
		System.setProperty(FilePreferencesFactory.SYSTEM_PROPERTY_FILE, ".preferences");

		SwingUtilities.invokeLater(new Gui());
	}

	public void run() {
		ConnectionFrame frame = new ConnectionFrame();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
