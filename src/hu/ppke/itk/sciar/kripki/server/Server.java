package hu.ppke.itk.sciar.kripki.server;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
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
		System.out.println("Server started.");
		Database db = new XMLDatabase("db/users.xml", "db/users");
		System.out.println("Database open.");

		// expect conn
		//  DH exchange
		// expect auth
		// 		reply with all records

		Server server = new Server(db);

		Document reply = server.handleRequest(new String(Files.readAllBytes(Paths.get("example.xml"))));

		System.out.println(OutputUtil.indentedString(reply, 2));
	}

	private final Database db;
	private Server(Database db) {
		this.db = db;
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
