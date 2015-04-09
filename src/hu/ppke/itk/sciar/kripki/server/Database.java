package hu.ppke.itk.sciar.kripki.server;


interface Database {
	public User getUser(String name);
	public User addUser(String name, String verifier);
	public void addRecord(User user, Record record);
	public org.w3c.dom.Document allRecords(User user);
}
