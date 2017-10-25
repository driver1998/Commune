package com.commune.stream;

import org.dom4j.*;

public interface InfoQueryElement extends DataElement {
    static InfoQueryElement getElement(String xmlString) throws InvalidElementException {
        try {
            Document document = DocumentHelper.parseText(xmlString);
            Element iqElement = document.getRootElement();
            Element queryElement = iqElement.element("query");
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

        } catch (DocumentException ex) {
            throw new InvalidElementException("格式错误");
        }
    }

}
