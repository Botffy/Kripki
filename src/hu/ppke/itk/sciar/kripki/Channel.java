package hu.ppke.itk.sciar.kripki;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.math.BigInteger;
import java.util.Random;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import org.w3c.dom.*;
import org.apache.commons.codec.digest.DigestUtils;
import net.sf.practicalxml.OutputUtil;
import net.sf.practicalxml.ParseUtil;
import net.sf.practicalxml.XmlException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Channel implements Closeable {
	private final static Logger log = LoggerFactory.getLogger("Root.CHANNEL");

	private final Random rand = new SecureRandom();
	private final DataInputStream in;
	private final DataOutputStream out;
	private final SocketAddress address;

	public Channel(String host, int port) throws IOException {
		this(new Socket(host, port));
	}

	public Channel(Socket sock) throws IOException {
		this.in = new DataInputStream(sock.getInputStream());
		this.out = new DataOutputStream(sock.getOutputStream());
		this.address = sock.getRemoteSocketAddress();
	}

	private byte[] readBytes() throws IOException {
		int len = in.readInt();
		log.trace("Message size {}", len);
		byte[] msg = new byte[len];

		int got = 0;
		while(got < len) {
			int nowgot = in.read(msg, got, len-got);
			got+=nowgot;
			log.trace("Recieved {} bytes of {}", got, len);
		}

		log.trace("Message {}", new String(msg));
		return msg;
	}

	private void writeBytes(byte[] msg) throws IOException {
		out.writeInt(msg.length);
		out.write(msg, 0, msg.length);
	}

	private String serializeXML(Document dom) {
		return String.format("%s\n%s", "<?xml version='1.0' encoding='UTF-8' ?>", OutputUtil.indentedString(dom, 3));
	}

	public Document readMessage() throws IOException {
		String str = new String(readBytes(), java.nio.charset.StandardCharsets.UTF_8);
		try {
			return ParseUtil.parse(str);
		} catch(XmlException e) {
			log.error("Couldn't parse XML. Exact message was:\n {}", str);
			throw e;
		}
	}

	public void writeMessage(Document msg) throws IOException {
		writeBytes((serializeXML(msg)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public byte[] sharedKey(BigInteger sharedSecret) {
		return Arrays.copyOf(DigestUtils.sha1(sharedSecret.toByteArray()), 16);
	}

	public void writeCiphered(Document msg, byte[] key) throws IOException {
		assert key.length == 16;

		try {
			byte[] input = (serializeXML(msg)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
			byte[] iv = new byte[16];
			rand.nextBytes(iv);

			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));

			out.write(iv, 0, 16);
			writeBytes(cipher.doFinal(input));
		} catch(GeneralSecurityException e) {
			throw new IOException(String.format("Enciphering error: '%s'", e.getMessage()), e);
		}
	}

	public Document readCiphered(byte[] key) throws IOException {
		assert key.length == 16;

		try {
			byte[] iv = new byte[16];
			in.read(iv);	//assumption, much. fixme, eventually
			int len = in.readInt();
			byte[] ciphertext = new byte[len];
			in.read(ciphertext);

			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
			return ParseUtil.parse(new String(cipher.doFinal(ciphertext),"UTF-8"));
		} catch(GeneralSecurityException e) {
			throw new IOException(String.format("Deciphering error: '%s'", e.getMessage()), e);
		}
	}

	@Override public void close() throws IOException {
		out.close();
	}

	public SocketAddress getRemoteAddress() {
		return address;
	}
}
