package hu.ppke.itk.sciar.kripki.server;

import java.util.Deque;
import java.util.ArrayDeque;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.apache.commons.lang3.StringEscapeUtils;


public class Server {
	public static void main(String[] args) throws Exception {
		System.out.println("Server started.");
		Database db = new XMLDatabase("db/users.xml", "db/users");
		System.out.println("Database open.");

		// expect conn
		// expect auth
		// 		reply with all records
		// client MAY add new record
		//		reply with all records
		

		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();

		RequestHandler handler = new RequestHandler(db);
		xmlReader.setContentHandler(handler);
		xmlReader.setErrorHandler(handler);

		String reply;
		try {
			xmlReader.parse("example.xml");
			reply = handler.getReply();
		} catch(RequestHandler.Error e) {
			reply = e.getReply();
		}
		System.out.println(reply);
	}

	private static class RequestHandler extends DefaultHandler {
		public class Error extends SAXException {
			private final String type;
			private Error(String type, String msg) {
				super(msg);
				this.type = type;
			}

			public String getReply() {
				return String.format("<error type='%s'>%s</error>", StringEscapeUtils.escapeXml11(type), StringEscapeUtils.escapeXml11(getMessage()));
			}
		}

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
					throw new Error("user/auth", "Could not authorize user.");
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
				throw new Error("malformed", String.format("Unexpected request element: %s", qName));
			}
		}
		@Override public void endElement(String uri, String localName, String qName) throws SAXException {
			if(!stack.peek().equalsIgnoreCase(qName)) {
				throw new Error("malformed", "Element mismatch");
			}
			stack.removeFirst();

			if(stack.isEmpty() && !user.equals(User.noneSuch) && reply.isEmpty()) {
				reply = db.allRecords(user);
			}
		}

		@Override public void fatalError(SAXParseException e) throws RequestHandler.Error {
			throw new Error("malformed", e.getMessage());
		}

		public String getReply() {
			return reply;
		}
	}
}
