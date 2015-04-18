package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;
import hu.ppke.itk.sciar.utils.swing.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.List;


class RecordForm extends JDialog {
	private CardLayout cards = new CardLayout();


	public RecordForm(JFrame parent) {
		this(parent, null);
	}

	public RecordForm(JFrame parent, Record record) {
		super(parent, record==null? "Add new record" : String.format("Edit record for %s", record.url), true);

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.weightx = 1;
		constr.weighty = 1;
		constr.insets = new Insets(4,4,4,4);

		constr.gridx = 0;
		constr.gridy = 0;
		constr.gridwidth = 1;
		form.add(new JLabel("URL:"), constr);
		constr.gridx = 1;
		constr.gridwidth = 3;
		form.add(new JTextField(20), constr);

		constr.gridx = 0;
		constr.gridy = 1;
		constr.gridwidth = 1;
		form.add(new JLabel("Username:"), constr);
		constr.gridx = 1;
		constr.gridwidth = 3;
		form.add(new JTextField(20), constr);

		constr.gridx = 0;
		constr.gridy = 2;
		constr.gridwidth = 1;
		form.add(new JLabel("Password:"), constr);
		constr.gridx = 1;
		constr.gridwidth = 3;
		form.add(new JTextField(20), constr);

		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.LINE_AXIS));
		btnPanel.setBorder(BorderFactory.createEmptyBorder(10 , 10, 10, 10));
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.add(new JButton("Add"));
		btnPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		btnPanel.add(new JButton("Cancel"));

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(form, BorderLayout.CENTER);
		mainPanel.add(btnPanel, BorderLayout.PAGE_END);


		JPanel curtain = new JPanel();
		curtain.setVisible(false);
		curtain.setLayout(new BorderLayout());
		JProgressBar spinner = new JProgressBar();
		spinner.setIndeterminate(true);
		curtain.add(spinner, BorderLayout.CENTER);

		this.getContentPane().setLayout(cards);
		this.getContentPane().add(mainPanel);
		this.getContentPane().add(curtain);

		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
	}
}
