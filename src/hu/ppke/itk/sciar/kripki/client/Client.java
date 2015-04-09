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
	}

	private DiffieHellman dh;
	public Client() {
	}

	public boolean connect(String host, int port) {
		this.dh = new DiffieHellman(1024, 2);

		DataOutputStream out;
		DataInputStream in;
		try {
			log.info("Connecting to {}:{}...", host, port);
			Socket socket = new Socket(host, port);
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
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
					XmlBuilder.element("modulus", XmlBuilder.text(Integer.toString(this.dh.modulusBitLength())))
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
			log.info("Successfully agreed on shared key: {}", Hex.encodeHexString( Protocol.sharedKey(dh.sharedSecret(theirResult)) ));

		} catch(Exception e) {
			log.error("Couldn't do DH key-exchange: {}", e.getMessage());
			return false;
		}

	return true;
	}


/*
			log.info("Sending testdata.");
			byte[] msg = Files.readAllBytes(Paths.get("example.xml"));
			out.writeInt(msg.length);
			out.write(msg, 0, msg.length);

			log.info("Awaiting reply.");
			int len = in.readInt();
			log.trace("Expect {} bytes of message", len);
			msg = new byte[len];
			int got = in.read(msg, 0, len);
			log.trace("Got {} bytes of message", got);

			Document reply = ParseUtil.parse(new String(msg, StandardCharsets.UTF_8));
			log.debug("Got reply.");

			System.out.println(OutputUtil.indentedString(reply, 3));
*/
}
