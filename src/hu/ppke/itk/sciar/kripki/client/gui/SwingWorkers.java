package hu.ppke.itk.sciar.kripki.client.gui;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.kripki.client.*;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;


class SwingWorkers {
	public static abstract class AuthWorker extends javax.swing.SwingWorker<List<Record>, String> {
		protected final Client client;

		public AuthWorker(Client client) {
			this.client = client;
		}

		@Override protected List<Record> doInBackground() throws Exception {
			client.connect();
			return client.authenticate();
		}
	}


	public static abstract class DataRetriever extends javax.swing.SwingWorker<List<Record>, String> {
		protected final Client client;

		public DataRetriever(Client client) {
			this.client = client;
		}

		@Override protected List<Record> doInBackground() throws Exception {
			return client.getData();
		}
	}


	public static abstract class RecordSender extends DataRetriever {
		private final Record record;
		public RecordSender(Client client, Record record) {
			super(client);
			this.record = record;
		}

		@Override protected List<Record> doInBackground() throws Exception {
			return client.addRecord(record);
		}
	}
}
