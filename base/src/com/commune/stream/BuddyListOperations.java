package com.commune.stream;

import com.commune.model.User;
import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BuddyListOperations implements InfoQueryElement{
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

    public static final String OPERATION_NS_QUERY = "iq:buddy:query";
    public static final String OPERATION_NS_SEARCH = "iq:buddy:search";
    public static final String OPERATION_NS_ADD = "iq:buddy:add";
    public static final String OPERATION_NS_DELETE = "iq:buddy:remove";

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

            Element iqElement = document.createElement("iq");
            document.appendChild(iqElement);

            iqElement.setAttribute("id", id);

            if (from != null) iqElement.setAttribute("from", from.getName());
            if (to != null) iqElement.setAttribute("to", to.getName());

            Element queryElement = document.createElementNS(operation, "query");
            iqElement.appendChild(queryElement);

            if (operation.equals(BuddyListOperations.OPERATION_NS_SEARCH))
                queryElement.setAttribute("keyword", keyword);

            if (items != null) {
                for (User u : items) {
                    Element itemElement = document.createElement("item");
                    itemElement.setNodeValue(u.getName());
                    queryElement.appendChild(itemElement);
                }
            }
            return Util.getXmlString(document);
        } catch (ParserConfigurationException | IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    static BuddyListOperations parseXML(Element iqElement) {
        String id = iqElement.getAttribute("id");
        String fromID = iqElement.getAttribute("from");
        String toID = iqElement.getAttribute("to");

        User from = null;
        User to = null;
        if (fromID != null && !fromID.isEmpty()) from = new User(fromID);
        if (toID != null && !toID.isEmpty()) to = new User(toID);

        Element queryElement = (Element) iqElement.getElementsByTagName("query").item(0);
        String operation = queryElement.getNamespaceURI();

        String keyword = queryElement.getAttribute("keyword");

        List<User> users = new ArrayList<>();

        NodeList elements =  queryElement.getElementsByTagName("item");
        if (elements!=null) {
            int length = elements.getLength();

            for (int i=0; i<length; i++) {
                Element e = (Element) elements.item(i);
                users.add(new User(e.getNodeValue()));
            }
        }

        if (users.isEmpty()) users = null;
        return new BuddyListOperations(from, to, id, operation, keyword, users);
    }

}
