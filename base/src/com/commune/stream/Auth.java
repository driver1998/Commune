package com.commune.stream;

import com.commune.utils.Util;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Auth implements InfoQueryElement {
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

            Document document = builder.newDocument();


            Element iqElement = document.createElement("iq");
            document.appendChild(iqElement);

            Attr idAttr = document.createAttribute("id"); idAttr.setValue(id);
            iqElement.getAttributes().setNamedItem(idAttr);

            Element queryElement = document.createElementNS("iq:auth", "query");
            iqElement.appendChild(queryElement);

            Element usernameElement = document.createElement("username");
            usernameElement.setNodeValue(username);
            queryElement.appendChild(usernameElement);

            Element hashElement = document.createElement("hash");
            hashElement.setNodeValue(hash1);
            queryElement.appendChild(hashElement);



            return Util.getXmlString(document);
        } catch (ParserConfigurationException | IOException ex) {
            ex.printStackTrace();
            return "";
        }

    }

    static Auth parseXML(Element iqElement) {
        String id = iqElement.getAttribute("id");

        Element queryElement = (Element)iqElement.getElementsByTagName("query").item(0);

        Element usernameElement = (Element) queryElement.getElementsByTagName("username").item(0);
        String username = usernameElement.getNodeValue();

        Element hashElement = (Element) queryElement.getElementsByTagName("hash").item(0);
        String hash1 = hashElement.getNodeValue();

        return new Auth(username, hash1, id);
    }
}
