package com.commune.stream;
import com.commune.model.User;
import org.dom4j.*;

public class Message implements DataElement {

    User from;
    User to;
    String body;

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

    static Message parseXML(org.dom4j.Element messageElement) throws InvalidElementException {
        String fromName = messageElement.attributeValue("from");
        String toName = messageElement.attributeValue("to");

        if (fromName.isEmpty()) throw new InvalidElementException("from为空");
        if (toName.isEmpty()) throw new InvalidElementException("to为空");

        User from = new User(fromName);
        User to = new User(toName);

        Element bodyElement = messageElement.element("body");
        String body = bodyElement.getText();

        return new Message(from, to, body);
    }

    public String getXML(){
        Element messageElement = DocumentHelper.createElement("message")
                .addAttribute("from", from.getName())
                .addAttribute("to", to.getName());

        messageElement.addElement("body")
                .setText(body);

        return messageElement.asXML();
    }

}