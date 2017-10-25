package com.commune.stream;

import com.commune.model.User;
import org.dom4j.DocumentHelper;

public class Presence implements DataElement {
    User from;
    User to;
    String id;
    String type;

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
        org.dom4j.Element presenceElement = DocumentHelper.createElement("presence")
                .addAttribute("from", from.getName());

        if (!type.equals(TYPE_AVAILABLE)) presenceElement.addAttribute("type", type);

        if (to != null) presenceElement.addAttribute("to", to.getName());

        if (id != null) presenceElement.addAttribute("id", id);

        return presenceElement.asXML();
    }

    static Presence parseXML(org.dom4j.Element presenceElement) throws InvalidElementException {

        String type = presenceElement.attributeValue("type");
        if (type == null) type = TYPE_AVAILABLE;
        String fromName = presenceElement.attributeValue("from");
        String toName = presenceElement.attributeValue("to");
        String id = presenceElement.attributeValue("id");

        if (fromName.isEmpty()) throw new InvalidElementException("from为空");

        User from = new User(fromName);
        User to = new User(toName);

        return new Presence(from, to, type, id);
    }
}
