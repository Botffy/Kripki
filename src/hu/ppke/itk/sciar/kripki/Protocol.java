package hu.ppke.itk.sciar.kripki;

import java.io.*;
import java.util.Arrays;
import java.math.BigInteger;
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
}
