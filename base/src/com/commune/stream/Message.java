package com.commune.stream;

import com.commune.model.User;
import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Message implements DataElement {

    private User from;
    private User to;
    private String body;

    public User getFrom() {
        return from;
    }
    public User getTo() {
        return to;
    }
    public String getBody() {
        return body;
    }

    public Message(User from, User to, String body) {
        this.from = from;
        this.to = to;
        this.body = body;
    }

    public String getXML(){
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.newDocument();

            Element messageElement = document.createElement("message");
            document.appendChild(messageElement);

            messageElement.setAttribute("from", from.getName());
            messageElement.setAttribute("to", to.getName());


            Element bodyElement = document.createElement("body");
            bodyElement.setNodeValue(body);
            messageElement.appendChild(bodyElement);

            return Util.getXmlString(document);
        } catch (ParserConfigurationException | IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    static Message parseXML(Element messageElement) throws InvalidElementException {
        String fromName = messageElement.getAttribute("from");
        String toName = messageElement.getAttribute("to");

        if (fromName.isEmpty()) throw new InvalidElementException("from为空");
        if (toName.isEmpty()) throw new InvalidElementException("to为空");

        User from = new User(fromName);
        User to = new User(toName);

        Element bodyElement = (Element)messageElement.getElementsByTagName("body").item(0);
        String body = bodyElement.getNodeValue();

        return new Message(from, to, body);
    }


}