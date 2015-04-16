package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;
import hu.ppke.itk.sciar.utils.swing.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import org.w3c.dom.Document;


class ListingFrame extends JFrame {
	private final Client client;
	public ListingFrame(Client client) {
		super("Kripki");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.client = client;
	}
}
