package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;


class DocumentRetriever extends javax.swing.SwingWorker<List<Record>, String> {
	private final Client client;

	private String statusMessage;
	public DocumentRetriever(Client client) {
		this.client = client;
		this.statusMessage = "connecting...";
	}

	@Override protected List<Record> doInBackground() throws Exception {
		List<Record> Result = client.getData();
		return Result;
	}

	public String getStatusMessage() {
		return statusMessage;
	}
}
