package hu.ppke.itk.sciar.kripki.server;


import java.io.File;
import java.util.Scanner;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.practicalxml.ParseUtil;


class XMLDatabase implements Database {
	private final Document doc;
	private final File usersFile;
	private final File usersLib;
	private final Transformer transformer = TransformerFactory.newInstance().newTransformer();
	private final DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

	public XMLDatabase(String usersFile, String usersLib) throws Exception {
		this(new File(usersFile), new File(usersLib));
	}

	public XMLDatabase(File usersFile, File usersLib) throws Exception {
		this.usersFile = usersFile;
		this.usersLib  = usersLib;

		if(!usersFile.exists()) {
			doc = documentBuilder.newDocument();
			doc.appendChild(doc.createElement("users"));
			flush(doc, usersFile);
		} else if(usersFile.isDirectory()) {
			throw new RuntimeException(String.format("usersfile %s exists and is a directory", usersFile));
		} else {
			doc = documentBuilder.parse(usersFile);
			doc.getDocumentElement().normalize();
		}

		if(!usersLib.exists()) {
			if(!usersLib.mkdirs()) throw new RuntimeException(String.format("Could not create userLib %s", usersLib));
		} else if(!usersLib.isDirectory())  {
			throw new RuntimeException(String.format("userLib %s exists and is not a directory", usersLib));
		}

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
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

	public boolean userAuth(User user) {
		User stor = getUser(user.name);
		return stor.verifier.equals(user.verifier);
	}

	@Override public User addUser(String name, String verifier) {
		if(name.isEmpty() || verifier.isEmpty()) throw new RuntimeException("Tried to add empty user");
		if(!getUser(name).equals(User.noneSuch)) throw new RuntimeException("User already exists");

		Element us = doc.createElement("user");
		us.setAttribute("name", name);
		us.setAttribute("verifier", verifier);
		doc.getDocumentElement().appendChild(us);
		flush(doc, usersFile);
		return new User(name, verifier);
	}

	private File getUserFile(User user) {
		File ufil = new File(usersLib, user.name+".xml");
		if(!ufil.exists())	{
			Document udoc = documentBuilder.newDocument();
			Element root = udoc.createElement("user");
			root.setAttribute("name", user.name);
			root.setAttribute("verifier", user.verifier);
			udoc.appendChild(root);
			flush(udoc, ufil);
		}
		return ufil;
	}

	private Document getUserXML(User user) {
		File ufil = getUserFile(user);
		Document udoc;
		try {
			udoc = documentBuilder.parse(ufil);
			udoc.getDocumentElement().normalize();
		} catch(Exception e) {
			throw new RuntimeException(String.format("could not parse file %s", ufil), e);
		}
		return udoc;
	}

	@Override public void addRecord(User user, Record record) {
		assert userAuth(user);

		Document udoc = getUserXML(user);

		// record exists?
		NodeList list = udoc.getDocumentElement().getElementsByTagName("record");
		Element rec = null;
		for(int i=0; i<list.getLength(); ++i) {
			if( list.item(i).getNodeType() != Node.ELEMENT_NODE ) continue;
			if( ((Element)list.item(i)).getAttribute("url").equals(record.url) ) {
				rec = (Element) list.item(i);
				Node sib = rec.getPreviousSibling();
				if(sib.getNodeType() == Node.TEXT_NODE) sib.getParentNode().removeChild(sib);
				rec = (Element) rec.getParentNode().removeChild(rec);
			}
		}

		if(rec == null) {
			rec = udoc.createElement("record");
		}

		rec.setAttribute("url", record.url);
		rec.setAttribute("username", record.username);
		rec.setAttribute("passwd", record.password);
		rec.setAttribute("recordsalt", record.salt);
		udoc.getDocumentElement().appendChild(rec);

		flush(udoc, new File(usersLib, user.name+".xml"));
	}

	@Override public Document allRecords(User user) {
		assert userAuth(user);

		try {
			return ParseUtil.parse(getUserFile(user));
		} catch(Exception e) {
			throw new RuntimeException("This should never happen", e);
		}
	}

	private void flush(Document document, File file) {
		try {
			transformer.transform(new DOMSource(document), new StreamResult(file));
			System.out.println(String.format("Data flushed to %s.", file));
		} catch(TransformerException e) {
			throw new RuntimeException(String.format("Failed to flush to %s", file), e);
		}
	}
}
