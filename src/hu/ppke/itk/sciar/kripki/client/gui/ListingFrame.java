package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;
import hu.ppke.itk.sciar.utils.swing.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.List;


class ListingFrame extends JFrame {
	private final RecordTableModel model;
	public ListingFrame(Client client, List<Record> records) {
		super("Kripki");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.model = new RecordTableModel(records, client);

		Action addNewAction = new AddNewAction(this);

		final JTable table = new JTable(this.model);
		table.setCellSelectionEnabled(false);
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.getViewport().setPreferredSize(new Dimension(
			table.getPreferredScrollableViewportSize().width,
			table.getRowHeight()*10
		));

		final JPanel buttPane = new JPanel();
		buttPane.add(new JButton(addNewAction));
		//buttPane.add(new JButton("Edit selected"));

		this.getContentPane().add(scrollPane, BorderLayout.CENTER);
		this.getContentPane().add(buttPane, BorderLayout.SOUTH);
		this.setResizable(false);
		this.pack();
	}

	public void addNew() {
		RecordForm form = new RecordForm(this, model);
		form.setLocationRelativeTo(this);
		form.setVisible(true);
	}
}


class AddNewAction extends AbstractAction {
	private final ListingFrame listing;
	public AddNewAction(ListingFrame listing) {
		super("Add new");
		this.listing = listing;
	}

	@Override public void actionPerformed(ActionEvent ev) {
		this.listing.addNew();
	}
}
