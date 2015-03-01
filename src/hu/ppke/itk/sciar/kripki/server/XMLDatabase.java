package hu.ppke.itk.sciar.kripki.server;


import java.io.File;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


class XMLDatabase implements Database {
	private final Document doc;
	private final File usersFile;
	private final File usersLib;

	public XMLDatabase(String usersFile, String usersLib) throws Exception {
		this(new File(usersFile), new File(usersLib));
	}

	public XMLDatabase(File usersFile, File usersLib) throws Exception {
		this.usersFile = usersFile;
		this.usersLib  = usersLib;

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		if(!usersFile.exists()) {
			doc = builder.newDocument();
			doc.appendChild(doc.createElement("users"));
			flush();
		} else if(usersFile.isDirectory()) {
			throw new RuntimeException(String.format("usersfile %s exists and is a directory", usersFile));
		} else {
			doc = builder.parse(usersFile);
			doc.getDocumentElement().normalize();
		}
	}


	@Override public User getUser(String name) {
		NodeList users = doc.getDocumentElement().getElementsByTagName("user");
		for(int i=0; i<users.getLength(); ++i) {
			if( users.item(i).getNodeType() != Node.ELEMENT_NODE ) continue;
			Element e = (Element) users.item(i);

			if(e.getAttribute("name").equalsIgnoreCase(name)) {
				return new User(e.getAttribute("name"), e.getAttribute("verifier"));
			}
		}

		return User.noneSuch;
	}

	@Override public User addUser(String name, String verifier) {
		if(name.isEmpty() || verifier.isEmpty()) throw new RuntimeException("Tried to add empty user");
		if(!getUser(name).equals(User.noneSuch)) throw new RuntimeException("User already exists");

		Element us = doc.createElement("user");
		us.setAttribute("name", name);
		us.setAttribute("verifier", verifier);
		doc.getDocumentElement().appendChild(us);
		flush();
		return new User(name, verifier);
	}

	private void flush() {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(doc), new StreamResult(usersFile));
			System.out.println("XMLDatabase flushed.");
		} catch(TransformerException e) {
			throw new RuntimeException("Failed to flush changes to usersFile!", e);
		}
	}
}
