package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;
import hu.ppke.itk.sciar.utils.swing.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import org.apache.commons.lang3.StringUtils;


class ConnectionFrame extends JFrame {
	private final JTextField hostField;
	private final JTextField portField;
	private final JTextField userField;
	private final JPasswordField passField;
	private final JButton connectButt;

	CardLayout cards = new CardLayout();

	private final Client client;
	public ConnectionFrame(Client client) {
		super("Kripki");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.client = client;

		Action connectAction = new ConnectAction(this);

		hostField = new JTextField("localhost", 12);
        portField = new JTextField(5);
        PlainDocument doc = new PlainDocument();
		doc.setDocumentFilter(new DigitDocumentFilter());
        portField.setDocument(doc);
        portField.setText("1294");
        userField = new JTextField();
        passField = new JPasswordField();

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();
		Insets ins = new Insets(4,4,4,4);
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.insets = ins;

		constr.gridx = 0;
		constr.gridy = 0;
		constr.gridwidth = 1;
		constr.insets = ins;
		panel.add(new JLabel("Server:"), constr);

		constr.gridx = 1;
		constr.gridwidth = 3;
		panel.add(hostField, constr);

		constr.gridx = 1;
		constr.gridwidth = 1;
		panel.add(new JLabel(":"));

		constr.gridx = 5;
		constr.gridwidth = 1;
		panel.add(portField, constr);

		constr.gridx=0;
		constr.gridy=1;
		constr.gridwidth = 1;
		panel.add(new JLabel("Username:"), constr);

		constr.gridx=1;
		constr.gridy=1;
		constr.gridwidth = 6;
		panel.add(userField, constr);

		constr.gridx=0;
		constr.gridy=2;
		constr.gridwidth = 1;
		panel.add(new JLabel("Password:"), constr);

		constr.gridx=1;
		constr.gridy=2;
		constr.gridwidth = 6;
		panel.add(passField, constr);

		connectButt = new JButton(connectAction);
		JPanel btnPanel = new JPanel();
		btnPanel.add(connectButt, BorderLayout.SOUTH);

		JPanel loginForm = new JPanel();
		loginForm.add(panel, BorderLayout.CENTER);
		loginForm.add(btnPanel, BorderLayout.SOUTH);

		loginForm.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("pressed ENTER"), "connect");
		loginForm.getActionMap().put("connect", connectAction);

		JPanel curtain = new JPanel();
		curtain.setVisible(false);
		curtain.setLayout(new BorderLayout());
		JProgressBar spinner = new JProgressBar();
		spinner.setIndeterminate(true);
		curtain.add(spinner, BorderLayout.CENTER);

		this.getContentPane().setLayout(cards);
		this.getContentPane().add(loginForm);
		this.getContentPane().add(curtain);

		this.pack();
		this.setResizable(true);
		this.repaint();
	}

	public void doConnect() {
		List<String> errors = new ArrayList<String>();

		String hostStr = hostField.getText();
		String portStr = portField.getText();
		String userStr = userField.getText();
		if(StringUtils.isBlank(hostStr)) {
			errors.add("Hostname cannot be blank");
		}
		if(StringUtils.isBlank(userStr)) {
			errors.add("Username cannot be blank");
		}
		int port=0;
		try {
			port = Integer.valueOf(portStr);
		} catch(NumberFormatException e) {
			errors.add("Port number must be a number.");
		}
		char[] passStr = passField.getPassword();

		if(errors.isEmpty()) {
			User user = new User(userStr, new String(passStr));		//later this will be user = User.encryptedUser(userStr, passStr)
			Arrays.fill(passStr, '\0');

			cards.next(getContentPane());
			DocumentRetriever task = new DocumentRetriever(client, hostStr, port, user) {
				@Override protected void done() {
					try {
						System.out.println(this.get());
					} catch(Exception e) {
						cards.next(getContentPane());
						JOptionPane.showMessageDialog(ConnectionFrame.this,
							this.getStatusMessage(),
							"Error",
							JOptionPane.ERROR_MESSAGE
						);
					}
				}
			};
			task.execute();
		} else {
			JOptionPane.showMessageDialog(this,
				StringUtils.join(errors, '\n'),
				"Error",
				JOptionPane.ERROR_MESSAGE
			);
		}
	}
}

class ConnectAction extends AbstractAction {
	private final ConnectionFrame connFrame;
	public ConnectAction(ConnectionFrame connFrame) {
		super("Connect");
		this.connFrame = connFrame;
	}

	@Override public void actionPerformed(ActionEvent ev) {
		this.connFrame.doConnect();
	}
}
