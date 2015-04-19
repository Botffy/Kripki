package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;
import hu.ppke.itk.sciar.utils.swing.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.List;


class ListingFrame extends JFrame {
	private final RecordTableModel model;
	private final JTable table;
	private final JButton editButt;
	public ListingFrame(Client client, List<Record> records) {
		super("Kripki");
		setIconImage((new ImageIcon(ConnectionFrame.class.getResource("/res/key.png"))).getImage());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.model = new RecordTableModel(records, client);

		final Action addNewAction = new AddNewAction();
		final Action editSelectedAction = new EditSelectedAction();

		table = new JTable(this.model);
		table.setCellSelectionEnabled(false);
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().setValueIsAdjusting(false);
		table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				setBorder(noFocusBorder);
				return this;
			}
		});
		table.addMouseListener(new MouseAdapter() {		// edit on doubleclick
			@Override public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) editSelectedAction.actionPerformed(new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "edit"));
			}
		});
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override public void valueChanged(ListSelectionEvent e) {
				ListingFrame.this.tableSelectionChanged();
			}
		});
		table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("pressed ENTER"), "edit");			// edit on enter
		table.getActionMap().put("edit", editSelectedAction);
		table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
		table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
		final JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getViewport().setPreferredSize(new Dimension(
			table.getPreferredScrollableViewportSize().width,
			table.getRowHeight()*10
		));


		final JPanel buttPane = new JPanel();
		buttPane.add(new JButton(addNewAction));
		editButt = new JButton(editSelectedAction);
		buttPane.add(editButt);

		this.getContentPane().add(scrollPane, BorderLayout.CENTER);
		this.getContentPane().add(buttPane, BorderLayout.SOUTH);
		this.setResizable(false);
		this.addWindowListener(new WindowAdapter() {
			public void windowOpened( WindowEvent e ){
				table.requestFocus();
				table.changeSelection(0,0, false, false);
			}
		});

		this.pack();
	}

	private void tableSelectionChanged() {
		if(table.getSelectedRow() > -1) editButt.setEnabled(true);
		else editButt.setEnabled(false);
	}

	protected void addNew() {
		RecordForm form = new RecordForm(this, model);
		form.setLocationRelativeTo(this);
		form.setVisible(true);
	}

	protected void editSelected() {
		if(table.getSelectedRow()<0) return;
		RecordForm form = new RecordForm(this, model.getRecord(table.getSelectedRow()), model);
		form.setLocationRelativeTo(this);
		form.setVisible(true);
	}


	private class AddNewAction extends AbstractAction {
		public AddNewAction() {
			super("Add new");
		}

		@Override public void actionPerformed(ActionEvent ev) {
			ListingFrame.this.addNew();
		}
	}

	private class EditSelectedAction extends AbstractAction {
		public EditSelectedAction() {
			super("Edit selected");
		}

		@Override public void actionPerformed(ActionEvent ev) {
			ListingFrame.this.editSelected();
		}
	}
}
