package com.commune.stream;

import com.commune.model.User;
import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public class Presence implements DataElement {
    private User from;
    private User to;
    private String id;
    private String type;

    public static final String TYPE_AVAILABLE = "";
    public static final String TYPE_UNAVAILABLE = "unavailable";
    public static final String TYPE_SUBSCRIBE = "subscribe";
    public static final String TYPE_UNSUBSCRIBE = "unsubscribe";
    public static final String TYPE_ACCEPT = "accept";
    public static final String TYPE_REJECT = "reject";

    public User getFrom() {
        return from;
    }
    public User getTo() {
        return to;
    }
    public String getId() {
        return id;
    }
    public String getType() {
        return type;
    }

    public Presence(User from, User to, String type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public Presence(User from, User to, String type, String id) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.id = id;
    }


    public String getXML() {
        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.newDocument();

            Element presenceElement = document.createElement("presence");
            document.appendChild(presenceElement);

            presenceElement.setAttribute("from", from.getName());
            if (to != null) presenceElement.setAttribute("to", to.getName());
            if (!type.equals(TYPE_AVAILABLE)) presenceElement.setAttribute("type", type);
            if (id != null) presenceElement.setAttribute("id", id);


            return Util.getXmlString(document);
        } catch (ParserConfigurationException | IOException | TransformerException ex) {
            ex.printStackTrace();
            return "";
        }

    }

    static Presence parseXML(Element presenceElement) throws InvalidElementException {

        String type = presenceElement.getAttribute("type");
        if (type == null) type = TYPE_AVAILABLE;
        String fromName = presenceElement.getAttribute("from");
        String toName = presenceElement.getAttribute("to");
        String id = presenceElement.getAttribute("id");

        if (fromName.isEmpty()) throw new InvalidElementException("from为空");

        User from = new User(fromName);
        User to = new User(toName);

        return new Presence(from, to, type, id);
    }
}
