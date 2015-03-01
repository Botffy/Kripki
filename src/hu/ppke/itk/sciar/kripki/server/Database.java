package hu.ppke.itk.sciar.kripki.server;


interface Database {
	public User getUser(String name);
	public User addUser(String name, String verifier);
	public void addRecord(User user, Record record);
	public String allRecords(User user);
}
