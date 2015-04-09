package hu.ppke.itk.sciar.kripki.client;


import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.w3c.dom.*;
import net.sf.practicalxml.ParseUtil;
import net.sf.practicalxml.OutputUtil;
import net.sf.practicalxml.XmlException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Client {
	private final static Logger log = LoggerFactory.getLogger("Root.CLIENT");

	public static void main(String[] args) throws Exception {
		String host = "localhost";
		int port = 1294;

		try {
			log.info("Connecting to {}:{}...", host, port);
			Socket socket = new Socket(host, port);
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			DataInputStream in = new DataInputStream(socket.getInputStream());
			log.info("Connected.");

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
		} catch(ConnectException e) {
			log.info("Couldn't connect to {}:{} ('{}')", host, port, e.getMessage());
		} catch(XmlException e) {
			log.error("Got malformed XML from the server");
		}
	}
}
