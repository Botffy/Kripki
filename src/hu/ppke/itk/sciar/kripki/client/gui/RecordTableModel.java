package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;
import hu.ppke.itk.sciar.utils.swing.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;


class RecordTableModel extends AbstractTableModel {
	private Client client;
	private List<Record> data;
	private Comparator<Record> recordComparator = new Comparator<Record>() {
		@Override public int compare(Record one, Record other) {
			return one.url.compareTo(other.url);
		}
		@Override public boolean equals(Object that) {
			return this == that;
		}
	};

	public RecordTableModel(List<Record> init, Client client) {
		this.data = init;
		this.client = client;
		Collections.sort(data, recordComparator);
	}

	@Override public String getValueAt(int row, int column) {
		Record record = data.get(row);
		switch(column) {
			case 0: return record.url;
			case 1: return record.username;
			case 2: return record.password;
			default:
				return null;
		}
	}

	public Record getRecord(int row) {
		return data.get(row);
	}

	@Override public int getColumnCount() {
		return 3;
	}
	@Override public int getRowCount() {
		return data.size();
	}
	@Override public String getColumnName(int column) {
		switch(column) {
			case 0: return "URL";
			case 1: return "Username";
			case 2: return "Password";
			default:
				return "?";
		}
	}
	@Override public Class<?> getColumnClass(int column) {
		return String.class;
	}

	public boolean recordExistsFor(String url) {
		for(Record record : data) {
			if(record.url.equals(url)) return true;
		}
		return false;
	}

	public List<String> validateInput(String url, String user, String pass) {
		List<String> errors = new ArrayList<String>();
		if(StringUtils.isBlank(url)) {
			errors.add("URL cannot be blank");
		}
		if(StringUtils.isBlank(user)) {
			errors.add("Username cannot be blank");
		}
		if(StringUtils.isBlank(pass)) {
			errors.add("Password cannot be blank");
		}
		return errors;
	}


	private static enum Action {
		ADD,
		UPDATE
	}

	public boolean updateRecord(final RecordForm origin, Record record) {
		return perform(Action.UPDATE, origin, record);
	}

	public boolean addRecord(final RecordForm origin, Record record) {
		return perform(Action.ADD, origin, record);
	}

	private boolean perform(Action action, final RecordForm origin, Record record) {
		List<String> errors = validateInput(record.url,record.username,record.password);
		if(!errors.isEmpty()) {
			JOptionPane.showMessageDialog(
				origin,
				StringUtils.join(errors, '\n'),
				"Error",
				JOptionPane.ERROR_MESSAGE
			);
			return false;
		}

		if(recordExistsFor(record.url) && action==Action.ADD) {
			int n = JOptionPane.showConfirmDialog(
				origin,
				String.format("A record for %s already exists. Would you like to overwrite it?", record.url),
				"Overwrite existing record?",
				JOptionPane.YES_NO_OPTION
			);
			if(n == JOptionPane.NO_OPTION) return false;
		}

		origin.curtainDown();
		SwingWorker task = new SwingWorkers.RecordSender(client, record) {
			@Override protected void done() {
				try {
					data = this.get();
					Collections.sort(data, recordComparator);
					SwingUtilities.invokeLater(new Runnable() {
						@Override public void run() {
							origin.shutdown();
							fireTableDataChanged();
						}
					});
				} catch(Exception e) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override public void run() {
							origin.curtainUp();
							JOptionPane.showMessageDialog(
								origin,
								StringUtils.join(e.getMessage()),
								"Error",
								JOptionPane.ERROR_MESSAGE
							);
						}
					});
				}
			}
		};
		task.execute();

		return true;
	}
}
