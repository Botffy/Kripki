package hu.ppke.itk.sciar.kripki.server;

import java.util.Deque;
import java.util.ArrayDeque;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;


public class Server {
	public static void main(String[] args) throws Exception {
		System.out.println("Server started.");

		Database db = new XMLDatabase("db/users.xml", "db/users");

		System.out.println("Database open.");

		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser saxParser = spf.newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();

		RequestHandler handler = new RequestHandler(db);
		xmlReader.setContentHandler(handler);
		xmlReader.parse("example.xml");

		System.out.println(handler.getReply());
	}

	private static class RequestHandler extends DefaultHandler {
		private Deque<String> stack = new ArrayDeque<String>();
		private final Database db;
		private User user = null;
		private String reply = "";

		public RequestHandler(Database db) {
			this.db = db;
		}

		@Override public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
			if(stack.isEmpty()) {
				stack.addFirst(qName);
				user = db.getUser( attributes.getValue("name") );
				if(user.equals(User.noneSuch)) {
					System.out.println("Creating new user...");
					user = db.addUser( attributes.getValue("name"), attributes.getValue("verifier") );
				} else if(!user.verifier.equals(attributes.getValue("verifier"))) {
					reply = "<error type='user/auth' />";
					throw new SAXException("Failed to authenticate.");
				} else System.out.println(String.format("Authenticated as %s", user.name));
			} else if( stack.peek().equals("user") && "record".equalsIgnoreCase(qName) ) {
				stack.addFirst(qName);
				System.out.println("Add record");
				db.addRecord(user, new Record(
					attributes.getValue("url"),
					attributes.getValue("username"),
					attributes.getValue("passwd"),
					attributes.getValue("recordsalt")
				));
			} else {
				reply = "<error type='malformed' />";
				throw new SAXException("Malformed XML");
			}
		}
		@Override public void endElement(String uri, String localName, String qName) throws SAXException {
			if(!stack.peek().equalsIgnoreCase(qName)) {
				reply = "<error type='malformed' />";
				throw new SAXException("Malformed XML (end)");
			}
			stack.removeFirst();

			if(stack.isEmpty() && !user.equals(User.noneSuch) && reply.isEmpty()) {
				reply = db.allRecords(user);
			}
		}

		public String getReply() {
			return reply;
		}
	}
}
