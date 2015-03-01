package hu.ppke.itk.sciar.kripki.server;


interface Database {
	public User getUser(String name);
	public User addUser(String name, String verifier);
}
