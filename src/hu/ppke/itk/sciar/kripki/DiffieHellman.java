package hu.ppke.itk.sciar.kripki;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;
import java.math.BigInteger;
import org.apache.commons.lang3.StringUtils;


public class DiffieHellman {
	private static Map<Integer, BigInteger> loadModuli(String fname) {
		Map<Integer, BigInteger> Result = new HashMap<Integer, BigInteger>();
		try {
			List<String> lines = Files.readAllLines(Paths.get(fname), StandardCharsets.UTF_8);

			Pattern declaration = Pattern.compile("(\\d+).*");
			boolean declare = true;
			Integer bits = null;
			List<String> valparts = new ArrayList<String>();
			for(String line : lines) {
				if(declare && !StringUtils.isBlank(line)) {
					Matcher match = declaration.matcher(line);
					if(!match.matches()) throw new RuntimeException(String.format("In %s expected modulus declaration, got '%s'", fname, line));
					bits = Integer.valueOf(match.group(1));
					declare = false;
				} else if(!declare) {
					if(StringUtils.isBlank(line)) {	// finish up
						String val = StringUtils.join(valparts, "");
						if(StringUtils.isBlank(val)) throw new RuntimeException(String.format("Empty value for bit %d in %s", bits, fname));
						if(val.length()*4 != bits) throw new RuntimeException(String.format("Was promised %d bits, got %s bits in %s", bits, val.length()*4, fname));

						Result.put(bits, new BigInteger(val, 16));

						valparts = new ArrayList<String>();
						bits = null;
						declare = true;
					} else { // gobble more value parts
						valparts.addAll( Arrays.asList(StringUtils.split(line, " ")) );
					}
				}
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}

		return Result;
	}

	public static void main(String[] args) throws Exception  {
		Map<Integer, BigInteger> moduli = loadModuli("res/DHmods.txt");
		for(Map.Entry<Integer, BigInteger> entry : moduli.entrySet()) {
			System.out.println(entry.getKey());
			System.out.println(entry.getValue());
		}
	}

	private static final Map<Integer, BigInteger> moduli = DiffieHellman.loadModuli("res/DHMods.txt");


	private final int primitiveElement;
	private final BigInteger modulus;
	public DiffieHellman(int modulusBit, int primitiveElement) {
		this.primitiveElement = primitiveElement;

		if(moduli.containsKey(modulusBit)) throw new IllegalArgumentException(String.format("I don't know a DH modulus of size %d", modulusBit));

		this.modulus = moduli.get(modulusBit);
	}
}