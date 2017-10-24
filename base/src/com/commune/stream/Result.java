package com.commune.stream;

import org.dom4j.*;

public class Result implements InfoQueryElement {
    String to;
    String id;
    String type;
    String body;

    public String getTo() {
        return to;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getBody() {
        return body;
    }

    public Result(String to, String id, String type, String body) {
        this.to = to;
        this.id = id;
        this.type = type;
        this.body = body;
    }

    public static final String TYPE_ERROR = "error";
    public static final String TYPE_INFORMATION = "info";
    public static final String BODY_INVALID_ELEMENT = "Invalid element";
    public static final String BODY_LOGIN_SUCCESS = "Login success";
    public static final String BODY_LOGIN_WRONG_INFO = "Incorrect username or password";
    public static final String BODY_INVALID_USER = "Invalid username";

    public String getXML() {
        Namespace namespace = DocumentHelper.createNamespace("", "iq:result");
        Element iqElement = DocumentHelper.createElement("iq").addAttribute("id", id);

        if (!to.isEmpty()) iqElement.addAttribute("to", to);

        Element queryElement = iqElement.addElement(new QName("query", namespace));

        Element typeElement = queryElement.addElement(type);
        typeElement.setText(body);

        return iqElement.asXML();
    }

    public static Result parseXML(Element iqElement) {
        String to = iqElement.attributeValue("to");
        String id = iqElement.attributeValue("id");

        Element queryElement = iqElement.element("query");

        Element typeElement = queryElement.elements().get(0);
        String type = typeElement.getName();

        String body = typeElement.getText();

        return new Result(to, id, type, body);
    }

}
