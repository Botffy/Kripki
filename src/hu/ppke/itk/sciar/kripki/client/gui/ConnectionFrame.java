package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;
import hu.ppke.itk.sciar.utils.swing.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.prefs.*;

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
	private final JComboBox<Integer> modSizeBox;
	private final JButton connectButt;

	private CardLayout cards = new CardLayout();

	private final Preferences prefs;

	public ConnectionFrame() {
		super("Kripki");
		setIconImage((new ImageIcon(ConnectionFrame.class.getResource("/res/key.png"))).getImage());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		prefs = Preferences.userNodeForPackage(this.getClass());

		Action connectAction = new ConnectAction();

		hostField = new JTextField( prefs.get("HOST", "localhost") , 12);
        portField = new JTextField(5);
        PlainDocument doc = new PlainDocument();
		doc.setDocumentFilter(new DigitDocumentFilter());
        portField.setDocument(doc);
        portField.setText( Integer.toString(prefs.getInt("PORT", 1294)) );
        userField = new JTextField( prefs.get("USERNAME", "") );
        passField = new JPasswordField();

        modSizeBox = new JComboBox<Integer>(DiffieHellman.getModulusSizes().toArray(new Integer[0]));
        modSizeBox.setSelectedItem(prefs.getInt("DHMODSIZE", Client.DIFFIEHELLMAN_DEFAULT_MODULUS_BIT));

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

		constr.gridx=0;
		constr.gridy=3;
		constr.gridwidth = 4;
		panel.add(new JLabel("Diffie-Hellman modulus size:"), constr);

		constr.gridx=4;
		constr.gridy=3;
		constr.gridwidth = 3;
		panel.add(modSizeBox, constr);


		connectButt = new JButton(connectAction);
		JPanel btnPanel = new JPanel();
		btnPanel.add(connectButt, BorderLayout.SOUTH);

		JPanel loginForm = new JPanel();
		loginForm.add(panel, BorderLayout.CENTER);
		loginForm.add(btnPanel, BorderLayout.SOUTH);

		loginForm.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("pressed ENTER"), "connect");
		loginForm.getActionMap().put("connect", connectAction);

		JPanel curtain = new Curtain();
		curtain.setVisible(false);

		this.getContentPane().setLayout(cards);
		this.getContentPane().add(loginForm);
		this.getContentPane().add(curtain);


		this.addWindowListener(new WindowAdapter() {
			public void windowOpened( WindowEvent e ){
				if(StringUtils.isBlank(userField.getText())) userField.requestFocus();
				else passField.requestFocus();
			}
		});

		this.pack();
		this.setResizable(false);
		this.repaint();
	}

	public void error(String msg) {
		cards.first(getContentPane());
		JOptionPane.showMessageDialog(
			this,
			msg,
			"Error",
			JOptionPane.ERROR_MESSAGE
		);
	}

	public void doConnect() {
		List<String> errors = new ArrayList<String>();

		final String hostStr = hostField.getText();
		final String portStr = portField.getText();
		final String userStr = userField.getText();
		final int dhModSize = modSizeBox.getItemAt(modSizeBox.getSelectedIndex());
		if(StringUtils.isBlank(hostStr)) {
			errors.add("Hostname cannot be blank");
		}
		if(StringUtils.isBlank(userStr)) {
			errors.add("Username cannot be blank");
		}
		int tmp_port = 0;
		try {
			tmp_port = Integer.valueOf(portStr);
		} catch(NumberFormatException e) {
			errors.add("Port number must be a number.");
		}
		final int port = tmp_port; //damn
		char[] passStr = passField.getPassword();

		if(errors.isEmpty()) {
			Client client = new Client(userStr, passStr, hostStr, port, dhModSize);
			Arrays.fill(passStr, '\0');

			cards.next(getContentPane());
			SwingWorker task = new KripkiWorker.AuthWorker(client) {
				@Override protected void done() {
					try {
						final List<Record> result = this.get();
						ConnectionFrame.this.prefs.put("HOST", hostStr);
						ConnectionFrame.this.prefs.putInt("PORT", port);
						ConnectionFrame.this.prefs.put("USERNAME", userStr);
						ConnectionFrame.this.prefs.putInt("DHMODSIZE", dhModSize);
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								Frame listing = new ListingFrame(client, result);
								listing.setLocationRelativeTo(ConnectionFrame.this);
								listing.setVisible(true);
								ConnectionFrame.this.setVisible(false);
								ConnectionFrame.this.dispose();
							}
						});
					} catch(Exception e) {
						ConnectionFrame.this.error(e.getMessage());
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


	private class ConnectAction extends AbstractAction {
		public ConnectAction() {
			super("Connect");
		}

		@Override public void actionPerformed(ActionEvent ev) {
			ConnectionFrame.this.doConnect();
		}
	}
}
