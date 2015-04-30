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
		log.info("Starting server.");
		log.info("Available DH modulus sizes are {}", DiffieHellman.getModulusSizes());

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
	private User authUser;
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
			if(!DiffieHellman.modulusExistsForSize(modulusBit)) {
				log.error("Client wanted to use unknown modulus size {}", modulusBit);
				channel.writeMessage(error("dh", String.format("I don't know any modulus of size %d", modulusBit)));
				return;
			}
			log.debug("Requested modulus size is {}", modulusBit);

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
		} catch(Exception e) {
			log.error("Diffie-Hellman key exchange failed, with error {}", e.getMessage());
			return;
		}

		try {
			authenticate();
			if(authUser == null) {
				channel.close();
				return;
			}

			channel.writeCiphered(db.allRecords(authUser), sharedKey);

			while(true) {
				try {
					handleRequest();
				} catch(RuntimeException e) {
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

	public void authenticate() throws IOException {
		assert sharedKey != null;

		log.info("Authenticating...");

		String username;
		String verifier;
		try {
			Document request = channel.readCiphered(sharedKey);
			Element userElement = DomUtil.getChild(request.getDocumentElement(), "user");	// users/user

			username = userElement.getAttribute("name");
			verifier = userElement.getAttribute("verifier");

			if(StringUtils.isBlank(username) || StringUtils.isBlank(verifier)) {
				log.error("Malformed XML: name or verifier blank");
				channel.writeCiphered(error("xml", "Malformed XML: name or verifier was blank"), sharedKey);
				return;
			}
		} catch(XmlException e) {
			log.error("XML error: {}", e.getMessage());
			channel.writeCiphered(error("xml", e.getMessage()), sharedKey);
			return;
		} catch(IOException e) {
			log.error("IO error: {}", e.getMessage());
			channel.writeCiphered(error("channel", e.getMessage()), sharedKey);
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
		authUser = user;
	}

	public void handleRequest() throws IOException {
		assert sharedKey != null;
		assert authUser != null;

		Document request = channel.readCiphered(sharedKey);
		Element userElement = request.getDocumentElement();	// user, theoretically
		log.info("Request from {}", authUser);

		for(Element elem : DomUtil.getChildren(userElement)) {
			if("record".equalsIgnoreCase(elem.getTagName())) {
				Record record = new Record(
					elem.getAttribute("url"),
					elem.getAttribute("username"),
					elem.getAttribute("passwd"),
					elem.getAttribute("recordsalt")
				);

				if(
					StringUtils.isBlank(record.url) ||
					StringUtils.isBlank(record.username) ||
					StringUtils.isBlank(record.password) ||
					StringUtils.isBlank(record.salt)
				) {
					log.info("{} tried to push invalid record (some data blank)", authUser.name);
					channel.writeCiphered(error("data", "Invalid record (some data blank)"), sharedKey);
					return;
				}
				log.info("{} creating/updating record for {}@{}", authUser.name, record.username, record.url);
				db.addRecord(authUser, record);
			} else if("delrecord".equalsIgnoreCase(elem.getTagName())) {
				Record record = new Record(
					elem.getAttribute("url"),
					elem.getAttribute("username"),
					elem.getAttribute("passwd"),
					elem.getAttribute("recordsalt")
				);
				log.info("{} deleting record {}", authUser.name, record);
				db.deleteRecord(authUser, record);
			} else {
				log.info("Unknown user action '{}'", elem.getTagName());
			}
		}

		channel.writeCiphered(db.allRecords(authUser), sharedKey);
	}


	private static Document error(String type, String message) {
		return XmlBuilder.element("error",
			XmlBuilder.attribute("type", type),
			XmlBuilder.text(message)
		).toDOM();
	}
}
