package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;
import hu.ppke.itk.sciar.utils.swing.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.List;
import java.util.ArrayList;


class RecordForm extends JDialog {
	private final RecordTableModel records;

	private CardLayout cards = new CardLayout();
	private boolean curtainIsDown = false;

	private final JTextField urlField;
	private final JTextField userField;
	private final JTextField passField;

	public RecordForm(JFrame parent, RecordTableModel records) {
		this(parent, null, records);
	}

	public RecordForm(JFrame parent, Record record, RecordTableModel records) {
		super(parent, record==null? "Add new record" : String.format("Edit record for %s", record.url), true);
		this.records = records;

		Action cancelAction = new CancelAction();
		Action doAction;
		if(record==null) doAction = new DoAction();
		else doAction = new UpdateAction();

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.weightx = 1;
		constr.weighty = 1;
		constr.insets = new Insets(4,4,4,4);

		urlField = new JTextField(20);
		constr.gridx = 0;
		constr.gridy = 0;
		constr.gridwidth = 1;
		form.add(new JLabel("URL:"), constr);
		constr.gridx = 1;
		constr.gridwidth = 3;
		if(record == null) {
			form.add(urlField, constr);
		} else {
			form.add(new JLabel(record.url), constr);
			urlField.setText(record.url);	// FIXME: hackish.
		}
		form.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("pressed ENTER"), "pleaseDo");
		form.getActionMap().put("pleaseDo", doAction);

		userField = new JTextField(20);
		if(record != null) {
			userField.setText(record.username);
		}
		constr.gridx = 0;
		constr.gridy = 1;
		constr.gridwidth = 1;
		form.add(new JLabel("Username:"), constr);
		constr.gridx = 1;
		constr.gridwidth = 3;
		form.add(userField, constr);

		passField = new JTextField(20);
		if(record != null) {
			passField.setText(record.password);
		}
		constr.gridx = 0;
		constr.gridy = 2;
		constr.gridwidth = 1;
		form.add(new JLabel("Password:"), constr);
		constr.gridx = 1;
		constr.gridwidth = 3;
		form.add(passField, constr);

		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.LINE_AXIS));
		btnPanel.setBorder(BorderFactory.createEmptyBorder(10 , 10, 10, 10));
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.add(new JButton(doAction));
		btnPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		btnPanel.add(new JButton(cancelAction));

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

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new FrameClosedEventAction(cancelAction));
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
		getRootPane().getActionMap().put("cancel", cancelAction);
		setResizable(false);
		pack();
	}

	private void addRecord() {
		String urlStr = urlField.getText();
		String userStr = userField.getText();
		String passStr = passField.getText();

		records.addRecord(this, new Record(urlStr, userStr, passStr, ""));
	}

	private void updateRecord() {
		String urlStr = urlField.getText();	//hack, much
		String userStr = userField.getText();
		String passStr = passField.getText();

		records.updateRecord(this, new Record(urlStr, userStr, passStr, ""));
	}

	private void cancel() {
		if(!curtainIsDown) shutdown();
	}

	void curtainDown() {
		cards.last(getContentPane());
		curtainIsDown = true;
	}
	void curtainUp() {
		cards.first(getContentPane());
		curtainIsDown = false;
	}
	void shutdown() {
		setVisible(false);
		dispose();
	}


	private class CancelAction extends AbstractAction {
		public CancelAction() {
			super("Cancel");
		}

		@Override public void actionPerformed(ActionEvent ev) {
			RecordForm.this.cancel();
		}
	}

	private class DoAction extends AbstractAction {
		public DoAction() {
			super("OK");
		}

		@Override public void actionPerformed(ActionEvent ev) {
			RecordForm.this.addRecord();
		}
	}

	private class UpdateAction extends AbstractAction {
		public UpdateAction() {
			super("OK");
		}

		@Override public void actionPerformed(ActionEvent ev) {
			RecordForm.this.updateRecord();
		}
	}
}
