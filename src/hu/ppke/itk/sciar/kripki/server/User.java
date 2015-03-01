package hu.ppke.itk.sciar.kripki.server;


class User {
	public final String name;
	public final String verifier;

	public User(String name, String verifier) {
		this.name = name;
		this.verifier = verifier;
	}

	@Override public String toString() {
		return String.format("%s (%s)", name, verifier);
	}
}
