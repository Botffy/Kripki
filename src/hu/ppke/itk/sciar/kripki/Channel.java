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


public class Channel {
	private final Random rand = new SecureRandom();
	private final DataInputStream in;
	private final DataOutputStream out;
	public Channel(DataInputStream in, DataOutputStream out) {
		this.in = in;
		this.out = out;
	}

	private byte[] readBytes() throws IOException {
		int len = in.readInt();
		byte[] msg = new byte[len];
		int got = in.read(msg, 0, len);
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
		return ParseUtil.parse(new String(readBytes(), java.nio.charset.StandardCharsets.UTF_8));
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
			throw new RuntimeException(String.format("Enciphering error: '%s'", e.getMessage()), e);
		}
	}

	public Document readCipheredXml(byte[] key) throws IOException {
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