package hu.ppke.itk.sciar.kripki.client;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.utils.ByteUtil;
import java.io.*;
import java.net.*;
import java.util.Arrays;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Client {
	private final static Logger log = LoggerFactory.getLogger("Root.CLIENT");

	public static void main(String[] args) throws Exception {
		String host="localhost";
		String portstr = "1294";
		int port;

		if(args.length>0) {
			host = args[0];
			if(host.contains(":")) {
				portstr = host.substring(host.indexOf(':')+1);
				host = host.substring(0, host.indexOf(':'));
			}
			else if(args.length>1) {
				portstr = args[1];
			}
		}

		try {
			port = Integer.valueOf(portstr);
		} catch(NumberFormatException e) {
			System.err.println(String.format("'%s' doesn't seem like a valid port number", portstr));
			return;
		}


		Client client = new Client("mormota", "atomrom".getBytes("UTF-8"));
		if(client.connect(host, port)) {
			Document data = client.addRecord(
				new Record("hottentotta.hu", "mormó", "1234?", "salt")
			);

			System.out.println(OutputUtil.indentedString(data, 3));
		}
	}

	private Socket socket;
	private Channel channel;
	private byte[] sharedKey = null;

	private final User user;
	private final byte[] masterKey;
	public Client(String username, byte[] password) {
		this.masterKey = DigestUtils.sha1(password);
		Arrays.fill(password, (byte)0);
		byte[] verifier = DigestUtils.sha1(masterKey);
		this.user = new User(username, Base64.encodeBase64String(verifier));
		Arrays.fill(verifier, (byte)0);
	}
	public Client(String username, char[] password) {
		this(username, ByteUtil.toBytes(password));
	}

	public boolean connect(String host, int port) {
		DiffieHellman dh = new DiffieHellman(1024, 2);

		try {
			log.info("Connecting to {}:{}...", host, port);
			this.socket = new Socket(host, port);
			this.channel = new Channel(
				new DataInputStream(socket.getInputStream()),
				new DataOutputStream(socket.getOutputStream())
			);
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
			channel.writeMessage(
				XmlBuilder.element("dh",
					XmlBuilder.element("step", XmlBuilder.text("1")),
					XmlBuilder.element("modulus", XmlBuilder.text(Integer.toString(dh.modulusBitLength())))
				).toDOM()
			);
			log.debug("Getting server's reply...");
			Document dhReply = channel.readMessage();
			BigInteger theirResult = new BigInteger(DomUtil.getText(DomUtil.getChild(dhReply.getDocumentElement(), "myresult")), 10);
			log.debug("Sending our result...");
			channel.writeMessage(
				XmlBuilder.element("dh",
					XmlBuilder.element("step", XmlBuilder.text("3")),
					XmlBuilder.element("myresult", XmlBuilder.text(dh.myResult().toString(10)))
				).toDOM()
			);
			sharedKey = channel.sharedKey(dh.sharedSecret(theirResult));
			log.info("Successfully agreed on shared key: {}", Hex.encodeHexString( sharedKey ));

		} catch(Exception e) {
			log.error("Couldn't do DH key-exchange: {}", e.getMessage());
			return false;
		}

	return true;
	}

	public Document getData() throws IOException {
		assert socket!=null && socket.isConnected();
		assert sharedKey != null;

		log.info("Requesting data for {}", user);
		channel.writeCiphered(
			XmlBuilder.element("user",
				XmlBuilder.attribute("name", user.name),
				XmlBuilder.attribute("verifier", user.verifier)
			).toDOM(),
			sharedKey
		);
		log.info("Fetching reply...");

		return channel.readCipheredXml(sharedKey);
	}

	public Document addRecord(Record record) throws IOException {
		assert socket != null && socket.isConnected();
		assert sharedKey != null;

		log.info("Sending new record {} for user {}", record, user);
		channel.writeCiphered(
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

		return channel.readCipheredXml(sharedKey);
	}

	public static boolean isError(Document doc) {
		 return "error".equals(doc.getDocumentElement().getTagName());
	}

	public static String errorString(Document doc) {
		assert isError(doc);
		return DomUtil.getText(doc.getDocumentElement());
	}
}
