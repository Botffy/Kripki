package hu.ppke.itk.sciar.kripki;

import org.apache.commons.lang3.builder.HashCodeBuilder;


public class User {
	public final String name;
	public final String verifier;

	public User(String name, String verifier) {
		this.name = name;
		this.verifier = verifier;
	}

	@Override public String toString() {
		return String.format("%s (%s)", name, verifier);
	}

	@Override public boolean equals(Object obj) {
		if(!(obj instanceof User)) return false;
		User that = (User) obj;
		return this.name.equals(that.name) && this.verifier.equals(that.verifier);
	}

	@Override public int hashCode() {
		return new HashCodeBuilder(17,31).append(name).append(verifier).toHashCode();
	}

	public static final User noneSuch = new User("", "");
}
