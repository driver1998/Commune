package com.commune.stream;

import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public interface InfoQueryElement extends DataElement {
    static InfoQueryElement getElement(String xmlString) throws InvalidElementException {
        try {

            Document document = Util.parseXmlString(xmlString);

            Element iqElement = document.getDocumentElement();
            Node queryElement = iqElement.getElementsByTagName("query").item(0);
            String namespace = queryElement.getNamespaceURI();

            switch (namespace) {
                case "iq:auth":
                    return Auth.parseXML(iqElement);
                case "iq:result":
                    return Result.parseXML(iqElement);
                case "iq:register":
                    return null;
                case BuddyListOperations.OPERATION_NS_ADD:
                case BuddyListOperations.OPERATION_NS_DELETE:
                case BuddyListOperations.OPERATION_NS_QUERY:
                case BuddyListOperations.OPERATION_NS_SEARCH:
                    return BuddyListOperations.parseXML(iqElement);
                default:
                    throw new InvalidElementException("格式错误");
            }

        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new InvalidElementException("格式错误");
        }
    }

}
