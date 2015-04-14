package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.client.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.GridLayout;


public class Gui implements Runnable {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Gui());
	}

	Client client;
	private Gui() {
		client = new Client();
	}

	public void run() {
		ConnectionFrame frame = new ConnectionFrame(client);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
