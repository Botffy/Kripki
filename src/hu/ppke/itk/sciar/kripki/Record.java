package hu.ppke.itk.sciar.kripki;


public class Record {
	public final String url;
	public final String username;
	public final String password;
	public final String salt;

	public Record(String url, String username, String password, String salt) {
		// fixme: check consistency or something?
		this.url = url;
		this.username = username;
		this.password = password;
		this.salt = salt;
	}

	@Override public String toString() {
		return String.format("(url=%s username=%s password=%s salt=%s)", url, username, password, salt);
	}

	public boolean overwrites(Record that) {
		return (this.url.equals(that.url) && this.username.equals(that.username));
	}

	@Override public boolean equals(Object obj) {
		if(!(obj instanceof Record)) return false;
		Record that = (Record) obj;
		return this.url.equals(that.url) && this.username.equals(that.username) && this.password.equals(that.password);
	}
}
