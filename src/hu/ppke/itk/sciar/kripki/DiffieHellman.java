package hu.ppke.itk.sciar.kripki;

import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.io.File;
import java.math.BigInteger;


public class DiffieHellman {
	static Map<Integer, BigInteger> moduli = new HashMap<Integer, BigInteger>();
	static {
		try {
			Scanner scanner = new Scanner(new File("res/DHmods.txt"));
			int r = scanner.nextInt(10);
			scanner.next("\\.\\*");
		} catch(Exception e) {
			throw new RuntimeException("Couldn't read the DH primes!", e);
		}

	}




}