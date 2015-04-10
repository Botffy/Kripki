package hu.ppke.itk.sciar.kripki;


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

	public static final User noneSuch = new User("", "");
}
