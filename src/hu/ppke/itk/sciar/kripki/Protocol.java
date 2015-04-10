package hu.ppke.itk.sciar.kripki;

import java.io.*;
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


public class Protocol {
	private Protocol() {}

	public static byte[] readMessage(DataInputStream in) throws IOException {
		int len = in.readInt();
		byte[] msg = new byte[len];
		int got = in.read(msg, 0, len);
		return msg;
	}

	public static String readStringMessage(DataInputStream in) throws IOException {
		return new String(Protocol.readMessage(in), java.nio.charset.StandardCharsets.UTF_8);
	}

	public static Document readXmlMessage(DataInputStream in) throws IOException {
		return  ParseUtil.parse(readStringMessage(in));
	}

	public static void writeMessage(DataOutputStream out, byte[] msg) throws IOException {
		out.writeInt(msg.length);
		out.write(msg, 0, msg.length);
	}

	public static void writeMessage(DataOutputStream out, String msg) throws IOException {
		writeMessage(out, msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public static void writeMessage(DataOutputStream out, Document msg) throws IOException {
		writeMessage(out, (serializeXML(msg)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	public static String serializeXML(Document dom) {
		return String.format("%s\n%s", "<?xml version='1.0' encoding='UTF-8' ?>", OutputUtil.indentedString(dom, 3));
	}

	public static byte[] sharedKey(BigInteger sharedSecret) {
		return Arrays.copyOf(DigestUtils.sha1(sharedSecret.toByteArray()), 16);
	}

	private static Random rand = new SecureRandom();
	public static void writeCiphered(DataOutputStream out, Document msg, byte[] key) throws IOException {
		assert key.length == 16;

		try {
			byte[] input = (serializeXML(msg)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
			byte[] iv = new byte[16];
			rand.nextBytes(iv);

			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));

			out.write(iv, 0, 16);
			writeMessage(out, cipher.doFinal(input));
		} catch(GeneralSecurityException e) {
			throw new RuntimeException(String.format("Enciphering error: '%s'", e.getMessage()), e);
		}
	}

	public static Document readCipheredXml(DataInputStream in, byte[] key) throws IOException {
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
			throw new RuntimeException(String.format("Deciphering error: '%s'", e.getMessage()), e);
		}
	}
}
