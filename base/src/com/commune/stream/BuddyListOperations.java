package com.commune.stream;

import com.commune.model.User;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import java.util.ArrayList;
import java.util.List;

public class BuddyListOperations implements InfoQueryElement{
    User from;
    User to;
    String id;
    String operation;
    List<User> items;

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

    public static final String OPERATION_NS_QUERY = "iq:buddy:query";
    public static final String OPERATION_NS_ADD = "iq:buddy:add";
    public static final String OPERATION_NS_DELETE = "iq:buddy:remove";

    public BuddyListOperations(User from, User to, String id, String operation, List<User> items) {
        this.items = items;
        this.operation = operation;
        this.id = id;
        this.from = from;
        this.to = to;
    }

    public String getXML() {
        Namespace namespace = DocumentHelper.createNamespace("", operation);
        Element iqElement = DocumentHelper.createElement("iq").addAttribute("id", id);

        if (from!=null)
            iqElement.addAttribute("from", from.getName());
        else
            if (to!=null) iqElement.addAttribute("to", to.getName());

        Element queryElement = iqElement.addElement(new QName("query", namespace));

        if (items != null) {
            for (User u : items) {
                queryElement.addElement("item").setText(u.getName());
            }
        }
        return iqElement.asXML();
    }

    public static BuddyListOperations parseXML(Element iqElement) {

        String id = iqElement.attributeValue("id");
        String fromID = iqElement.attributeValue("from");
        String toID = iqElement.attributeValue("to");

        User from = null;
        User to = null;
        if (fromID != null && !fromID.isEmpty())
            from = new User(fromID);
        else
            if (toID != null && !toID.isEmpty()) to = new User(toID);


        Element queryElement = iqElement.element("query");
        String operation = queryElement.getNamespaceURI();

        List<User> users = new ArrayList<>();

        List<Element> elements = queryElement.elements();
        if (elements!=null) {
            for (Element e : queryElement.elements()) {
                if (e.getName().equals("item")) {
                    users.add(new User(e.getText()));
                }
            }
        }

        if (users.isEmpty()) users = null;
        return new BuddyListOperations(from, to, id, operation, users);
    }

}
