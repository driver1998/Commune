package com.commune.stream;

import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public class Auth implements DataElement {
    private String username;
    private String hash1;
    private String id;

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }

    public String getHash1() {
        return hash1;
    }

    public Auth (String username, String hash1, String id) {
        this.username = username;
        this.hash1 = hash1;
        this.id = id;
    }

    public String getXML() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();


            Document document =builder.newDocument();


            Element authElement = document.createElement("auth");
            document.appendChild(authElement);

            authElement.setAttribute("id", id);

            Element usernameElement = document.createElement("username");
            usernameElement.setTextContent(username);
            authElement.appendChild(usernameElement);

            Element hashElement = document.createElement("hash");
            hashElement.setTextContent(hash1);
            authElement.appendChild(hashElement);

            return Util.getXmlString(document);
        } catch (ParserConfigurationException | IOException | TransformerException ex) {
            ex.printStackTrace();
            return "";
        }

    }

    static Auth parseXML(Element root) {
        String id = root.getAttribute("id");

        Element usernameElement = (Element) root.getElementsByTagName("username").item(0);
        String username = usernameElement.getTextContent();

        Element hashElement = (Element) root.getElementsByTagName("hash").item(0);
        String hash1 = hashElement.getTextContent();

        return new Auth(username, hash1, id);
    }
}
