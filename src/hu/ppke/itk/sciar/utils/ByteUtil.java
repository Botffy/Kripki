package hu.ppke.itk.sciar.utils;

import java.util.Arrays;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class ByteUtil {
	/**
		Convert char[] to byte[] destructively, without committing the string to the String Pool.
	*/
	public static byte[] toBytes(char[] chars, Charset charset) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = charset.encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000');
		Arrays.fill(byteBuffer.array(), (byte)0);
		return bytes;
	}

	public static byte[] toBytes(char[] chars) {
		return toBytes(chars, StandardCharsets.UTF_8);
	}

	/**
		Convert byte[] to char[] destructibely.
	*/
	public static char[] cloneToChars(byte[] bytes) {
		char[] Result = new char[bytes.length];
		for(int i=0; i<Result.length; ++i) {
			Result[i] = (char)bytes[i];
			bytes[i] = 0;
		}
		return Result;
	}

	/**
		Convert byte[] to char[] non-destructibely.
	*/
	public static char[] copyToChars(byte[] bytes) {
		char[] Result = new char[bytes.length];
		for(int i=0; i<Result.length; ++i) {
			Result[i] = (char)bytes[i];
		}
		return Result;
	}
}
