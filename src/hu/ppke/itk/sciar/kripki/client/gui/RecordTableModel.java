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
			int Result = one.url.compareTo(other.url);
			if(Result == 0) Result = one.username.compareTo(other.username);
			return Result;
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

	public Record getConflictingRecord(Record newRecord) {
		for(Record record : data) {
			if(newRecord.overwrites(record)) return record;
		}
		return null;
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

	public boolean updateRecord(final WorkerOrigin origin, Record record) {
		return perform(Action.UPDATE, origin, record);
	}

	public boolean addRecord(final WorkerOrigin origin, Record record) {
		return perform(Action.ADD, origin, record);
	}

	private boolean perform(Action action, final WorkerOrigin origin, Record record) {
		List<String> errors = validateInput(record.url,record.username,record.password);
		if(!errors.isEmpty()) {
			JOptionPane.showMessageDialog(
				origin.getComponent(),
				StringUtils.join(errors, '\n'),
				"Error",
				JOptionPane.ERROR_MESSAGE
			);
			return false;
		}

		Record conflicts = getConflictingRecord(record);
		if(conflicts!=null && action==Action.ADD) {
			int n = JOptionPane.showConfirmDialog(
				origin.getComponent(),
				String.format("A record for %s@%s already exists. Would you like to overwrite it?", record.username, record.url),
				"Overwrite existing record?",
				JOptionPane.YES_NO_OPTION
			);
			if(n == JOptionPane.NO_OPTION) return false;
		}
		if(conflicts!=null) {
			record = new Record(
				record.url,
				record.username,
				record.password,
				conflicts.salt  // to make sure the encoded url will be the same so that server will notice it's the same.
			);
		}

		origin.workerStarted();
		SwingWorker task = new KripkiWorker.RecordSender(client, record) {
			@Override protected void done() {
				workerDone(this, origin);
			}
		};
		task.execute();

		return true;
	}

	private void workerDone(final KripkiWorker worker, final WorkerOrigin origin) {
		try {
			data = worker.get();
			Collections.sort(data, recordComparator);
			SwingUtilities.invokeLater(new Runnable() {
				@Override public void run() {
					fireTableDataChanged();
					origin.workerSuccess();
				}
			});
		} catch(final Exception e) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override public void run() {
					origin.workerFailure();
					JOptionPane.showMessageDialog(
						origin.getComponent(),
						StringUtils.join(e.getMessage()),
						"Error",
						JOptionPane.ERROR_MESSAGE
					);
				}
			});
		}
	}
	public void refresh(final WorkerOrigin origin) {
		origin.workerStarted();
		SwingWorker task = new KripkiWorker.DataRetriever(client) {
			@Override protected void done() {
				workerDone(this, origin);
			}
		};
		task.execute();
	}

	public void deleteRecord(final WorkerOrigin origin, Record record) {
		int n = JOptionPane.showConfirmDialog(
			origin.getComponent(),
			String.format("Are you sure you would like to delete the record for %s@%s?", record.username, record.url),
			"Really delete?",
			JOptionPane.YES_NO_OPTION
		);
		if(n == JOptionPane.NO_OPTION) return;

		origin.workerStarted();
		SwingWorker task = new KripkiWorker.RecordDeleter(client, record) {
			@Override protected void done() {
				workerDone(this, origin);
			}
		};
		task.execute();
	}
}
