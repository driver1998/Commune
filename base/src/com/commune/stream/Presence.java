package com.commune.stream;

import com.commune.model.User;
import org.dom4j.DocumentHelper;

public class Presence implements DataElement {
    User from;
    User to;

    String type;

    public static final String TYPE_AVAILABLE = "";
    public static final String TYPE_UNAVAILABLE = "unavailable";
    public static final String TYPE_SUBSCRIBE = "subscribe";
    public static final String TYPE_UNSUBSCRIBE = "unsubscribe";

    public Presence(User from, User to, String type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public String getXML() {
        org.dom4j.Element presenceElement = DocumentHelper.createElement("presence")
                .addAttribute("from", from.getName());

        if (!type.equals(TYPE_AVAILABLE)) presenceElement.addAttribute("type", type);

        if (type.equals(TYPE_SUBSCRIBE) || type.equals(TYPE_UNSUBSCRIBE)) {
            if (to != null) presenceElement.addAttribute("to", to.getName());
        }

        return presenceElement.asXML();
    }

    static Presence parseXML(org.dom4j.Element presenceElement) throws InvalidElementException {

        String type = presenceElement.attributeValue("type");

        String fromName = presenceElement.attributeValue("from");

        String toName = presenceElement.attributeValue("to");

        if (fromName.isEmpty()) throw new InvalidElementException("to为空");

        User from = new User(fromName);
        User to = new User(toName);

        return new Presence(from, to, type);
    }
}
