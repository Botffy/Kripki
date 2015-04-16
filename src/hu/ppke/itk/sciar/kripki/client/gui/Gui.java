package hu.ppke.itk.sciar.kripki.client.gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.GridLayout;


public class Gui implements Runnable {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Gui());
	}

	public void run() {
		ConnectionFrame frame = new ConnectionFrame();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
