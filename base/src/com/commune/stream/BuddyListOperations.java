package com.commune.stream;

import com.commune.model.User;
import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BuddyListOperations implements DataElement{
    private User from;
    private User to;
    private String id;
    private String operation;
    private String keyword;
    private List<User> items;

    public String getId() {
        return id;
    }

    public User getFrom() {
        return from;
    }

    public List<User> getItems() {
        return items;
    }

    public String getOperation() {
        return operation;
    }

    public User getTo() {
        return to;
    }

    public String getKeyword() {
        return keyword;
    }

    public static final String OPERATION_NS_QUERY = "query";
    public static final String OPERATION_NS_SEARCH = "search";
    public static final String OPERATION_NS_ADD = "add";
    public static final String OPERATION_NS_DELETE = "remove";

    public BuddyListOperations(User from, User to, String id, String operation, List<User> items) {
        this.items = items;
        this.operation = operation;
        this.id = id;
        this.from = from;
        this.to = to;
    }

    public BuddyListOperations(User from, User to, String id, String operation, String keyword, List<User> items) {
        this.items = items;
        this.operation = operation;
        this.id = id;
        this.from = from;
        this.to = to;
        this.keyword = keyword;
    }

    public String getXML() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.newDocument();

            Element buddyElement = document.createElement("buddy");
            document.appendChild(buddyElement);


            if (from != null) buddyElement.setAttribute("from", from.getName());
            if (to != null) buddyElement.setAttribute("to", to.getName());
            buddyElement.setAttribute("type", operation);

            if (operation.equals(BuddyListOperations.OPERATION_NS_SEARCH))
                buddyElement.setAttribute("keyword", keyword);

            buddyElement.setAttribute("id", id);

            if (items != null) {
                for (User u : items) {
                    Element itemElement = document.createElement("item");
                    itemElement.setTextContent(u.getName());
                    buddyElement.appendChild(itemElement);
                }
            }
            return Util.getXmlString(document);
        } catch (ParserConfigurationException | IOException | TransformerException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    static BuddyListOperations parseXML(Element root) {
        String id = root.getAttribute("id");
        String fromID = root.getAttribute("from");
        String toID = root.getAttribute("to");
        String operation = root.getAttribute("type");
        String keyword = root.getAttribute("keyword");

        User from = null;
        User to = null;
        if (fromID != null && !fromID.isEmpty()) from = new User(fromID);
        if (toID != null && !toID.isEmpty()) to = new User(toID);

        List<User> users = new ArrayList<>();

        NodeList elements =  root.getElementsByTagName("item");
        if (elements!=null) {
            int length = elements.getLength();

            for (int i=0; i<length; i++) {
                Element e = (Element) elements.item(i);
                users.add(new User(e.getTextContent()));
            }
        }

        if (users.isEmpty()) users = null;
        return new BuddyListOperations(from, to, id, operation, keyword, users);
    }

}
