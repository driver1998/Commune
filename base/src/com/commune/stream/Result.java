package com.commune.stream;


import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public class Result implements DataElement {
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

            Element resultElement = document.createElement("result");
            document.appendChild(resultElement);

            if (!to.isEmpty()) resultElement.setAttribute("to", to);
            resultElement.setAttribute("id", id);
            resultElement.setAttribute("type", type);

            Element bodyElement = document.createElement("body");
            bodyElement.setTextContent(this.body);
            resultElement.appendChild(bodyElement);


            return Util.getXmlString(document);
        } catch (ParserConfigurationException | IOException | TransformerException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    static Result parseXML(Element root) {

        String to = root.getAttribute("to");
        String id = root.getAttribute("id");
        String type = root.getAttribute("type");

        Element messageElement =(Element) root.getElementsByTagName("body").item(0);
        String body = messageElement.getTextContent();

        return new Result(to, id, type, body);
    }

}
