package hu.ppke.itk.sciar.kripki.server;

import hu.ppke.itk.sciar.kripki.*;
import java.net.*;
import java.io.*;
import java.math.BigInteger;
import org.w3c.dom.*;
import net.sf.practicalxml.DomUtil;
import net.sf.practicalxml.ParseUtil;
import net.sf.practicalxml.OutputUtil;
import net.sf.practicalxml.XmlException;
import net.sf.practicalxml.builder.XmlBuilder;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.codec.binary.Hex;

import joptsimple.OptionParser;
import joptsimple.OptionSpec;
import joptsimple.OptionSet;
import joptsimple.OptionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Server implements Runnable {
	private final static Logger log = LoggerFactory.getLogger("Root.SERVER");

	public static void main(String[] args) throws Exception {
		OptionParser optionParser = new OptionParser();
		OptionSpec<Integer> portArg = optionParser.accepts( "port", "the port number the server listens to" )
			.withRequiredArg()
			.ofType(Integer.class)
			.defaultsTo(1294);
		OptionSpec<File> dbdirArg = optionParser.accepts("dbdir", "name of the database directory relative to cwd")
			.withRequiredArg()
			.ofType(File.class)
			.defaultsTo(new File("db"));

		OptionSet optionSet = null;
		int port;
		File dbdir;
		try {
			optionSet = optionParser.parse(args);
			port  = optionSet.valueOf(portArg);
			dbdir = optionSet.valueOf(dbdirArg);
		} catch(OptionException e) {
			System.out.println(String.format("Error: %s\nUsage: <prog-name> [--port=<port-number]>", e.getMessage()));
			return;
		}

		Database db = new XMLDatabase(dbdir);
		log.info("XMLDatabase open.");

		ServerSocket socket = new ServerSocket(port);
		log.info("Server started, listening on port {}", port);

		while(true) {
			Socket client = socket.accept();
			log.info("Accepted client connection from {}", client.getRemoteSocketAddress());
			(new Thread(new Server(client, db))).start();
		}
	}

	private final Database db;
	private final Channel channel;
	private byte[] sharedKey;
	private Server(Socket client, Database db) throws IOException {
		this.db = db;
		this.channel = new Channel(client);
		log.info("Started new thread to handle connection with {}", channel.getRemoteAddress());
	}

	public void run() {
		log.info("Expecting Diffie-Hellman initialization");

		try {
			Document dhInit = channel.readMessage();
			int modulusBit = Integer.valueOf(DomUtil.getText(DomUtil.getChild(dhInit.getDocumentElement(), "modulus")));
			DiffieHellman dh = new DiffieHellman(modulusBit, 2);

			log.debug("Replying with our result");
			channel.writeMessage(
				XmlBuilder.element("dh",
					XmlBuilder.element("step", XmlBuilder.text("2")),
					XmlBuilder.element("myresult", XmlBuilder.text(dh.myResult().toString(10)))
				).toDOM()
			);
			log.debug("Waiting for their result");
			Document dhReply = channel.readMessage();
			BigInteger theirResult = new BigInteger(DomUtil.getText(DomUtil.getChild(dhReply.getDocumentElement(), "myresult")), 10);
			this.sharedKey = channel.sharedKey(dh.sharedSecret(theirResult));
			log.info("Successfully agreed on shared key: {}", Hex.encodeHexString( sharedKey ));

			while(true) {
				try {
					handleRequest();
				} catch(Exception e) {
					log.error("Exception while handling the request: {}", e.getMessage());
					channel.writeCiphered(error("server", String.format("An unexpected error: '%s'", e.getMessage())), sharedKey);
					channel.close();
					break;
				}
			}
		} catch(IOException e) {
			log.info("Carrier lost: connection with {} lost", channel.getRemoteAddress());
		}
	}

	public void handleRequest() throws IOException {
		assert sharedKey != null;

		Document request = channel.readCiphered(sharedKey);

		log.info("Authenticating...");
		Element userElement = request.getDocumentElement();

		if("user".equals(userElement.getTagName())) {
			log.debug("Root was called 'user', this is nonstrict.");
		} else if("users".equals(userElement.getTagName())) {
			log.debug("Root was called 'users', this is strict.");
			userElement = DomUtil.getChild(userElement, "user");
			if(userElement == null) {
				log.info("Malformed request: root element 'users' had no 'user' child.");
				channel.writeCiphered(error("xml", "You gave me a 'users' root element which had no 'user' child"), sharedKey);
				return;
			}
		} else {
			log.info("Malformed XML: root element was '{}' (expected 'user' or 'users')", userElement.getTagName());
			channel.writeCiphered(error("xml", String.format("Malformed XML: root element was named '%s' (expected 'user' or 'users')", userElement.getTagName())), sharedKey);
			return;
		}

		String username = userElement.getAttribute("name");
		String verifier = userElement.getAttribute("verifier");

		if(StringUtils.isBlank(username) || StringUtils.isBlank(verifier)) {
			log.info("Malformed XML: name or verifier blank");
			channel.writeCiphered(error("xml", "Malformed XML: name or verifier was blank"), sharedKey);
			return;
		}

		User user;
		synchronized(db) {
			user = db.getUser(username);
			if(user == User.noneSuch) {
				log.info("No user called '{}'", username);
				user = db.addUser( username, verifier );
				log.info("Created new user '{}'", user.name);
			} else if(user.name.equals(username) && user.verifier.equals(verifier)) {
				log.info("Authenticated user {}", username);
			} else {
				log.info("Authentication failed for {}", username);
				channel.writeCiphered(error("user", "Could not authenticate user"), sharedKey);
				return;
			}
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

		channel.writeCiphered(db.allRecords(user), sharedKey);
	}


	private static Document error(String type, String message) {
		return XmlBuilder.element("error",
			XmlBuilder.attribute("type", type),
			XmlBuilder.text(message)
		).toDOM();
	}
}
