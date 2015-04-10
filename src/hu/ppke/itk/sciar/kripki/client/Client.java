package hu.ppke.itk.sciar.kripki.client;

import hu.ppke.itk.sciar.kripki.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import org.w3c.dom.*;
import net.sf.practicalxml.DomUtil;
import net.sf.practicalxml.ParseUtil;
import net.sf.practicalxml.OutputUtil;
import net.sf.practicalxml.XmlException;
import net.sf.practicalxml.builder.XmlBuilder;
import org.apache.commons.codec.binary.Hex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Client {
	private final static Logger log = LoggerFactory.getLogger("Root.CLIENT");

	public static void main(String[] args) throws Exception {
		Client client = new Client();
		client.connect("localhost", 1294);
		Document data = client.addRecord(
			new User("mormota", "atomrom"),
			new Record("hottentotta.hu", "mormó", "1234?", "salt")
		);

		System.out.println(OutputUtil.indentedString(data, 3));
	}

	private Socket socket;
	private DataOutputStream out;
	private DataInputStream in;
	private byte[] sharedKey = null;
	public Client() {
	}

	public boolean connect(String host, int port) {
		DiffieHellman dh = new DiffieHellman(1024, 2);

		try {
			log.info("Connecting to {}:{}...", host, port);
			this.socket = new Socket(host, port);
			this.out = new DataOutputStream(socket.getOutputStream());
			this.in = new DataInputStream(socket.getInputStream());
			log.debug("Connected.");
		} catch(SocketException e) {
			log.error("Couldn't connect to {}:{} ('{}')", host, port, e.getMessage());
			return false;
		} catch(IOException e) {
			log.error("Couldn't connect to {}:{} ('{}')", host, port, e.getMessage());
			return false;
		}

		try {
			log.info("Initiating Diffie-Hellman key exchange...");
			Protocol.writeMessage(out,
				XmlBuilder.element("dh",
					XmlBuilder.element("step", XmlBuilder.text("1")),
					XmlBuilder.element("modulus", XmlBuilder.text(Integer.toString(dh.modulusBitLength())))
				).toDOM()
			);
			log.debug("Getting server's reply...");
			Document dhReply = Protocol.readXmlMessage(in);
			BigInteger theirResult = new BigInteger(DomUtil.getText(DomUtil.getChild(dhReply.getDocumentElement(), "myresult")), 10);
			log.debug("Sending our result...");
			Protocol.writeMessage(out,
				XmlBuilder.element("dh",
					XmlBuilder.element("step", XmlBuilder.text("3")),
					XmlBuilder.element("myresult", XmlBuilder.text(dh.myResult().toString(10)))
				).toDOM()
			);
			sharedKey = Protocol.sharedKey(dh.sharedSecret(theirResult));
			log.info("Successfully agreed on shared key: {}", Hex.encodeHexString( sharedKey ));

		} catch(Exception e) {
			log.error("Couldn't do DH key-exchange: {}", e.getMessage());
			return false;
		}

	return true;
	}

	public Document getData(User user) throws IOException {
		assert socket!=null && socket.isConnected();
		assert sharedKey != null;

		log.info("Requesting data for {}", user);
		Protocol.writeCiphered(
			out,
			XmlBuilder.element("user",
				XmlBuilder.attribute("name", user.name),
				XmlBuilder.attribute("verifier", user.verifier)
			).toDOM(),
			sharedKey
		);
		log.info("Fetching reply...");

		return Protocol.readCipheredXml(in, sharedKey);
	}

	public Document addRecord(User user, Record record) throws IOException {
		assert socket != null && socket.isConnected();
		assert sharedKey != null;

		log.info("Sending new record {} for user {}", record, user);
		Protocol.writeCiphered(
			out,
			XmlBuilder.element("user",
				XmlBuilder.attribute("name", user.name),
				XmlBuilder.attribute("verifier", user.verifier),
				XmlBuilder.element("record",
					XmlBuilder.attribute("url", record.url),
					XmlBuilder.attribute("username", record.username),
					XmlBuilder.attribute("passwd", record.password),
					XmlBuilder.attribute("recordsalt", record.salt)
				)
			).toDOM(),
			sharedKey
		);
		log.info("Fetching reply...");

		return Protocol.readCipheredXml(in, sharedKey);
	}
}
