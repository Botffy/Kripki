package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;

import org.w3c.dom.Document;

import org.apache.commons.lang3.exception.ExceptionUtils;


class DocumentRetriever extends javax.swing.SwingWorker<Document, String> {
	private final Client client;

	private String statusMessage;
	public DocumentRetriever(Client client) {
		this.client = client;
		this.statusMessage = "connecting...";
	}

	@Override protected Document doInBackground() throws Exception {
		Document Result = client.getData();
		if(client.isError(Result)) {
			this.statusMessage = client.errorString(Result);
			throw new RuntimeException(statusMessage);
		}

		return Result;
	}

	public String getStatusMessage() {
		return statusMessage;
	}
}
