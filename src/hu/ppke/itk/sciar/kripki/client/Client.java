package hu.ppke.itk.sciar.kripki.client;

import hu.ppke.itk.sciar.kripki.*;
import hu.ppke.itk.sciar.utils.ByteUtil;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import org.w3c.dom.*;
import net.sf.practicalxml.DomUtil;
import net.sf.practicalxml.ParseUtil;
import net.sf.practicalxml.OutputUtil;
import net.sf.practicalxml.XmlException;
import net.sf.practicalxml.builder.XmlBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.Random;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Client {
	private final static Logger log = LoggerFactory.getLogger("Root.CLIENT");
	private final static int PBKDF2_ITER = 42;
	private final static String PASSWORD_SALT = "password";  // mm-hm.
	private final static String USERNAME_SALT = "userid";  // am I really doing this?
	private final static int DIFFIEHELLMAN_MODULUS_BIT = 1024;


	private Channel channel;

	private final String host;
	private final int port;
	private final User user;
	private final char[] masterKey;
	private byte[] sharedKey;
	public Client(String username, byte[] password, String host, int port) {
		this.host = host;
		this.port = port;

		byte[] master = DigestUtils.sha1(password);
		Arrays.fill(password, (byte)0);
		byte[] verifier = DigestUtils.sha1(master);
		this.user = new User(username, Base64.encodeBase64String(verifier));
		Arrays.fill(verifier, (byte)0);

		// keys have to be char[] unfortunately
		masterKey = ByteUtil.cloneToChars(master);

		log.info("Created {}", this.toString());
	}
	public Client(String username, char[] password, String host, int port) {
		this(username, ByteUtil.toBytes(password), host, port);
	}

	public void connect() throws IOException {
		byte[] key = null;
		DiffieHellman dh = new DiffieHellman(DIFFIEHELLMAN_MODULUS_BIT, 2);

		try {
			log.info("Connecting to {}:{}...", host, port);
			this.channel = new Channel(host, port);
			log.info("Connected.");
		} catch(IOException e) {
			log.error("Couldn't connect to {}:{} ('{}')", host, port, e.getMessage());
			throw new IOException(String.format("Could not connect to %s:%d (%s)", host, port, e.getMessage()), e);
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
			key = channel.sharedKey(dh.sharedSecret(theirResult));
			log.info("Successfully agreed on shared key: {}", Hex.encodeHexString(key));

		} catch(Exception e) {
			log.error("Couldn't do DH key-exchange: {}", e.getMessage());
			throw new IOException(String.format("Could not agree on a key with %s:%d.", host, port));
		}

	sharedKey = key;
	}

	public List<Record> authenticate() throws IOException {
		assert sharedKey != null;

		log.info("Trying to authenticate {}", this);

		channel.writeCiphered(
			XmlBuilder.element("users",
				XmlBuilder.element("user",
					XmlBuilder.attribute("name", user.name),
					XmlBuilder.attribute("verifier", user.verifier)
				)
			).toDOM(),
			sharedKey
		);

		return fetchReply(sharedKey);
	}

	public List<Record> getData() throws IOException {
		assert sharedKey != null;

		log.info("{} requesting data", this);

		channel.writeCiphered(
			XmlBuilder.element("user",
				XmlBuilder.attribute("name", user.name),
				XmlBuilder.attribute("verifier", user.verifier)
			).toDOM(),
			sharedKey
		);

		return fetchReply(sharedKey);
	}

	public List<Record> addRecord(Record record) throws IOException {
		assert sharedKey != null;

		log.info("{} sending {}", this, record);

		Record crypRecord = encryptRecord(record);
		channel.writeCiphered(
			XmlBuilder.element("user",
				XmlBuilder.attribute("name", user.name),
				XmlBuilder.attribute("verifier", user.verifier),
				XmlBuilder.element("record",
					XmlBuilder.attribute("url", crypRecord.url),
					XmlBuilder.attribute("username", crypRecord.username),
					XmlBuilder.attribute("passwd", crypRecord.password),
					XmlBuilder.attribute("recordsalt", crypRecord.salt)
				)
			).toDOM(),
			sharedKey
		);

		return fetchReply(sharedKey);
	}

	private List<Record> fetchReply(byte[] sharedKey) throws IOException {
		log.debug("Fetching reply...");
		Document doc = channel.readCiphered(sharedKey);
		log.debug("Reply recieved and decoded.");

		if(isError(doc)) throw new IOException(errorString(doc));

		List<Record> Result = new ArrayList<Record>();		//this is a bit of misuse of Record. maybe client.record and server.record should be separate?
		for(Element elem : DomUtil.getChildren(doc.getDocumentElement())) {
			if("record".equals(elem.getTagName())) {
				Record cryp = new Record(
					elem.getAttribute("url"),
					elem.getAttribute("username"),
					elem.getAttribute("passwd"),
					elem.getAttribute("recordsalt")
				);
				Record plain = decryptRecord(cryp);
				log.debug("Record for {}, decrypted as {}", cryp.url, plain);
				Result.add(plain);
			}
		}

		return Result;
	}

	/**
		Enrypts the password and username fields of given record.

		Salt is generated randomly, unless record.salt is not blank, in which case the original
		is supplied.
		Salt is also used as initial vector for the AEC/CBC encryption.
		Hostname is left unencrypted.
	*/
	public Record encryptRecord(Record record) throws IOException {
		Record Result = null;
		byte[] recordsalt = null;
		if(StringUtils.isBlank(record.salt)) {
			recordsalt = new byte[16];
			Random random = new SecureRandom();
			random.nextBytes(recordsalt);
		} else {
			recordsalt = Base64.decodeBase64(record.salt);
		}
		try {
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");

			PBEKeySpec recordspec = new PBEKeySpec(masterKey, recordsalt, PBKDF2_ITER, 128);
			SecretKey recordKey = secretKeyFactory.generateSecret(recordspec);
			recordspec.clearPassword(); // I'm feeling silly
			char[] recordKeyChars = ByteUtil.cloneToChars(recordKey.getEncoded());

			PBEKeySpec passpec = new PBEKeySpec(recordKeyChars, PASSWORD_SALT.getBytes(StandardCharsets.UTF_8), PBKDF2_ITER, 128);
			SecretKeySpec passKey = new SecretKeySpec(secretKeyFactory.generateSecret(passpec).getEncoded(), "AES");

			PBEKeySpec userspec = new PBEKeySpec(recordKeyChars, USERNAME_SALT.getBytes(StandardCharsets.UTF_8), PBKDF2_ITER, 128);
			SecretKeySpec userKey = new SecretKeySpec(secretKeyFactory.generateSecret(userspec).getEncoded(), "AES");
			userspec.clearPassword(); // really, why I am doing this?
			passpec.clearPassword(); // it's useless.
			Arrays.fill(recordKeyChars, '\0');

			cipher.init(Cipher.ENCRYPT_MODE, userKey, new IvParameterSpec(recordsalt));
			String username = Base64.encodeBase64String(cipher.doFinal(record.username.getBytes(StandardCharsets.UTF_8)));

			cipher.init(Cipher.ENCRYPT_MODE, passKey, new IvParameterSpec(recordsalt));
			String password = Base64.encodeBase64String(cipher.doFinal(record.password.getBytes(StandardCharsets.UTF_8)));

			Result = new Record(record.url, username, password, Base64.encodeBase64String(recordsalt));
		} catch(Exception e) {
			throw new IOException(String.format("Could not encrypt record: %s", e.getMessage()), e);
		}

		return Result;
	}

	/**
		Decrypts the username and password fields of given record.

		Salt is drawn from the original record & it's left untouched. Salt is also used as initial vector for th AEC/CBC
		encryption. Hostname is left untouched.
	*/
	public Record decryptRecord(final Record record) throws IOException {
		Record Result = null;
		byte[] recordsalt = Base64.decodeBase64(record.salt);
		try {
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");

			PBEKeySpec recordspec = new PBEKeySpec(masterKey, recordsalt, PBKDF2_ITER, 128);
			SecretKey recordKey = secretKeyFactory.generateSecret(recordspec);
			recordspec.clearPassword(); // this looks like good practice, but in effect it's perfectly useless, as keypeces later can't be destroyed.
			char[] recordKeyChars = ByteUtil.cloneToChars(recordKey.getEncoded());

			PBEKeySpec passpec = new PBEKeySpec(recordKeyChars, PASSWORD_SALT.getBytes(StandardCharsets.UTF_8), PBKDF2_ITER, 128);
			SecretKeySpec passKey = new SecretKeySpec(secretKeyFactory.generateSecret(passpec).getEncoded(), "AES");

			PBEKeySpec userspec = new PBEKeySpec(recordKeyChars, USERNAME_SALT.getBytes(StandardCharsets.UTF_8), PBKDF2_ITER,128);
			SecretKeySpec userKey = new SecretKeySpec(secretKeyFactory.generateSecret(userspec).getEncoded(), "AES");
			userspec.clearPassword();
			passpec.clearPassword();
			Arrays.fill(recordKeyChars, '\0');

			cipher.init(Cipher.DECRYPT_MODE, userKey, new IvParameterSpec(recordsalt));
			String username = new String(cipher.doFinal(Base64.decodeBase64(record.username)), StandardCharsets.UTF_8);

			cipher.init(Cipher.DECRYPT_MODE, passKey, new IvParameterSpec(recordsalt));
			String password = new String(cipher.doFinal(Base64.decodeBase64(record.password)), StandardCharsets.UTF_8);

			Result = new Record(record.url, username, password, record.salt);
		} catch(Exception e) {
			throw new IOException(String.format("Could not decrypt record: %s", e.getMessage()), e);
		}

		return Result;
	}

	public static boolean isError(Document doc) {
		 return "error".equals(doc.getDocumentElement().getTagName());
	}

	public static String errorString(Document doc) {
		assert isError(doc);
		return DomUtil.getText(doc.getDocumentElement());
	}

	@Override public String toString() {
		return String.format("%s@%s:%d", user.name, host, port);
	}
}
