package hu.ppke.itk.sciar.kripki.server;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.w3c.dom.*;
import net.sf.practicalxml.ParseUtil;
import net.sf.practicalxml.OutputUtil;
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
		Document req = ParseUtil.parse(new String(Files.readAllBytes(Paths.get("example.xml"))));
		String reply = server.handleRequest(req);
		System.out.println(reply);
	}

	private final Database db;
	private Server(Database db) {
		this.db = db;
	}

	public String handleRequest(Document request) {
		log.info("Authenticating...");
		Element userElement = request.getDocumentElement();
		if(!"user".equals(userElement.getTagName())) {
			log.info("Malformed XML: root element was {}", userElement.getTagName());
			return "error: malformed";
		}

		String username = userElement.getAttribute("name");
		String verifier = userElement.getAttribute("verifier");

		if(StringUtils.isBlank(username) || StringUtils.isBlank(verifier)) {
			log.info("Malformed XML: name or verifier blank");
			return "error: malformed";
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
			return "error: auth";
		}

		return "mightyfine";
	}
}
