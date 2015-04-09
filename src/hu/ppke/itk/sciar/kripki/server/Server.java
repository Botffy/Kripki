package hu.ppke.itk.sciar.kripki.server;

import java.net.*;
import java.io.*;
import org.w3c.dom.*;
import net.sf.practicalxml.DomUtil;
import net.sf.practicalxml.ParseUtil;
import net.sf.practicalxml.OutputUtil;
import net.sf.practicalxml.XmlException;
import net.sf.practicalxml.builder.XmlBuilder;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Server {
	private final static Logger log = LoggerFactory.getLogger("Root.SERVER");

	public static void main(String[] args) throws Exception {
		Database db = new XMLDatabase("db/users.xml", "db/users");
		log.info("XMLDatabase open.");

		int port = 1294;
		ServerSocket socket = new ServerSocket(port);
		log.info("Server started, listening on port {}", port);

		Socket client = socket.accept();
		log.info("Accepted client connection from {}", client.getRemoteSocketAddress());

		Server server = new Server(client, db);

		log.info("Closing server");
	}

	private final Database db;
	private final Socket client;
	private Server(Socket client, Database db) throws IOException {
		assert client.isConnected();
		this.db = db;
		this.client = client;

		DataOutputStream out = new DataOutputStream(client.getOutputStream());
		DataInputStream in = new DataInputStream(client.getInputStream());

		// expect len + lensize xml
		int len = in.readInt();
		log.trace("Expect {} bytes of message", len);
		byte[] msg = new byte[len];
		int got = in.read(msg, 0, len);
		log.trace("Got {} bytes of message", got);
		Document reply = handleRequest(new String(msg, java.nio.charset.StandardCharsets.UTF_8));
		System.out.println(OutputUtil.indentedString(reply, 2));
		log.info("Reply sent");
	}

	public Document handleRequest(String req) {
		Document request;
		try {
			request = ParseUtil.parse(req);
		} catch(XmlException e) {
			log.info("Malformed XML: {}", e.getMessage());
			return error("xml", e.getMessage());
		}

		log.info("Authenticating...");
		Element userElement = request.getDocumentElement();
		if(!"user".equals(userElement.getTagName())) {
			log.info("Malformed XML: root element was '{}' (expected 'user')", userElement.getTagName());
			return error("xml", String.format("Malformed XML: root element was named '%s' (expected 'user')", userElement.getTagName()));
		}

		String username = userElement.getAttribute("name");
		String verifier = userElement.getAttribute("verifier");

		if(StringUtils.isBlank(username) || StringUtils.isBlank(verifier)) {
			log.info("Malformed XML: name or verifier blank");
			return error("xml", "Malformed XML: name or verifier was blank");
		}

		User user = db.getUser(username);
		if(user == User.noneSuch) {
			log.info("No user called '{}'", username);
			user = db.addUser( username, verifier );
			log.info("Created new user '{}'", user.name);
		} else if(user.name.equals(username) && user.verifier.equals(verifier)) {
			log.info("Authenticated user {}", username);
		} else {
			log.info("Authentication failed for {}", username);
			return error("user", "Could not authenticate user");
		}

		for(Element elem : DomUtil.getChildren(userElement)) {
			if("record".equals(elem.getTagName())) {
				Record record = new Record(
					elem.getAttribute("url"),
					elem.getAttribute("username"),
					elem.getAttribute("passwd"),
					elem.getAttribute("recordsalt")
				);

				log.info("Creating/updating record for {}", record.url);
				db.addRecord(user, record);
			} else {
				log.info("Unknown user action '{}'", elem.getTagName());
			}
		}

		return db.allRecords(user);
	}


	private static Document error(String type, String message) {
		return XmlBuilder.element("error",
			XmlBuilder.attribute("type", type),
			XmlBuilder.text(message)
		).toDOM();
	}
}
