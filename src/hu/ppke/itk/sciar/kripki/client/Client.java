package hu.ppke.itk.sciar.kripki.client;


import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

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


		} catch(ConnectException e) {
			log.info("Couldn't connect to {}:{} ('{}')", host, port, e.getMessage());
			System.out.println(String.format("Couldn't connect to %s:%d: '%s'", host, port, e.getMessage()));
		}
	}
}
