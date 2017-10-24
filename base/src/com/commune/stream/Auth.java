package com.commune.stream;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

public class Auth implements InfoQueryElement {
    String username;
    String hash1;
    String id;

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
        Namespace namespace = DocumentHelper.createNamespace("", "iq:auth");
        Element iqElement = DocumentHelper.createElement("iq").addAttribute("id", id);

        Element queryElement = iqElement.addElement(new QName("query", namespace));

        queryElement.addElement("username").setText(username);
        queryElement.addElement("hash").setText(hash1);

        return iqElement.asXML();
    }

    public static Auth parseXML(Element iqElement) {
        String id = iqElement.attributeValue("id");

        Element queryElement = iqElement.element("query");

        Element usernameElement = queryElement.element("username");
        String username = usernameElement.getText();

        Element hashElement = queryElement.element("hash");
        String hash1 = hashElement.getText();

        return new Auth(username, hash1, id);
    }
}
