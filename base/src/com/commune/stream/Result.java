package com.commune.stream;


import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Result implements InfoQueryElement {
    private String to;
    private String id;
    private String type;
    private String body;

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
    public static final String BODY_FILE_ACCEPT = "File accepted";
    public static final String BODY_FILE_REJECT = "File objected";

    public String getXML() {
        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.newDocument();


            //Namespace namespace = DocumentHelper.createNamespace("", "iq:result");
            Element iqElement = document.createElement("iq");
            document.appendChild(iqElement);

            iqElement.setAttribute("id", id);

            if (!to.isEmpty()) iqElement.setAttribute("to", to);


            Element queryElement = document.createElementNS("query", "iq:result");
            iqElement.appendChild(queryElement);

            Element typeElement = document.createElement(type);
            typeElement.setNodeValue(body);
            queryElement.appendChild(typeElement);


            return Util.getXmlString(document);
        } catch (ParserConfigurationException | IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    static Result parseXML(Element iqElement) {
        String to = iqElement.getAttribute("to");
        String id = iqElement.getAttribute("id");

        Element queryElement =(Element) iqElement.getElementsByTagName("query").item(0);

        Element typeElement = (Element) queryElement.getChildNodes().item(0);

        String type = typeElement.getNodeName();
        String body = typeElement.getNodeValue();

        return new Result(to, id, type, body);
    }

}
