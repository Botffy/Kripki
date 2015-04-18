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
import java.util.Collections;
import java.util.Comparator;


class RecordTableModel extends AbstractTableModel {
	private List<Record> data;
	private Comparator<Record> recordComparator = new Comparator<Record>() {
		@Override public int compare(Record one, Record other) {
			return one.url.compareTo(other.url);
		}
		@Override public boolean equals(Object that) {
			return this == that;
		}
	};

	public RecordTableModel(List<Record> init) {
		this.data = init;
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
}
