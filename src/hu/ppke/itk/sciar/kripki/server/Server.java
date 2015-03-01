package hu.ppke.itk.sciar.kripki.server;


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

		RequestHandler handler = new RequestHandler();
		xmlReader.setContentHandler(handler);
		xmlReader.parse("example.xml");

		if(!handler.getState().isAccepting()) {
			System.out.println("<error type='malformed' />");
		} else {
			User user = db.getUser( handler.getUser().name );
			if(user.equals(User.noneSuch)) {
				System.out.println(String.format("No user named %s, creating it...", handler.getUser().name));
				user = db.addUser(handler.getUser().name, handler.getUser().verifier);
			}

			System.out.println("Authenticated as " + user.name);
		}
	}

	private static class RequestHandler extends DefaultHandler {
		public static enum State {
			START(false),
			ERROR(false),
			AUTH(true),
			ADD_RECORD(true);

			private final boolean accepting;
			private State(boolean accepting) {
				this.accepting = accepting;
			}

			public boolean isAccepting() {
				return accepting;
			}
		}

		private State state = State.START;
		private User user = null;

		@Override public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
			if(state == State.START) {
				if("user".equalsIgnoreCase(qName)) {
					state = State.AUTH;
					String name = attributes.getValue("name");
					String verifier = attributes.getValue("verifier");
					if(name==null || verifier==null) state = State.ERROR;
					else this.user = new User( name, verifier );
				}
				else state = State.ERROR;
			}
			else if(state == State.AUTH) {
				if("record".equalsIgnoreCase(qName)) {
					state = State.ADD_RECORD;
					String url = attributes.getValue("url");
					String username = attributes.getValue("username");
					String password = attributes.getValue("passwd");
					String salt = attributes.getValue("recordsalt");
					if(url==null || username==null || password==null || salt==null) state = State.ERROR;
				}
			}
			else state = State.ERROR;
		}

		public State getState() {
			return state;
		}
		public User getUser() {
			return user;
		}
	}
}
