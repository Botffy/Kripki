package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;

import org.w3c.dom.Document;

import org.apache.commons.lang3.exception.ExceptionUtils;


class DocumentRetriever extends javax.swing.SwingWorker<Document, String> {
	private final Client client;
	private final String host;
	private final int port;
	private final User user;

	private String statusMessage;
	public DocumentRetriever(Client client, String host, int port, User user) {
		this.client = client;
		this.host = host;
		this.port = port;
		this.user = user;
		this.statusMessage = "connecting...";
	}

	@Override protected Document doInBackground() throws Exception {
		if(!client.connect(host, port)) {
			statusMessage = "Could not connect to server";
			throw new RuntimeException(statusMessage);
		}
		Document Result = client.getData(user);
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
