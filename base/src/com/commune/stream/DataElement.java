package com.commune.stream;

import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.Socket;

public interface DataElement {

    String getXML();

    static DataElement getElement(String xmlString) throws InvalidElementException {
        try {

            Document document = Util.parseXmlString(xmlString);

            Element root = document.getDocumentElement();
            String name = root.getNodeName();

            switch (name) {
                case "message":
                    return Message.parseXML(root);
                case "presence":
                    return Presence.parseXML(root);
                case "iq":
                    return InfoQueryElement.getElement(xmlString);
                case "file":
                    return FileMessage.parseXML(root);
                default:
                    throw new InvalidElementException("格式错误");
            }

        } catch (IOException | SAXException | ParserConfigurationException ex) {
            System.out.println(xmlString);
            throw new InvalidElementException("格式错误");
        }
    }


    static DataElement getElement(Socket socket) throws IOException, InvalidElementException {
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

        String line;
        StringBuilder sb = new StringBuilder();


        while ((line=br.readLine())!=null) {
            if (line.equals("<break/>")) break;
            sb.append(line);
            System.out.println("recv " + line);
        }

        return getElement(sb.toString());
    }


    default void send(Socket socket) {
        try  {
            String xml = getXML();
            System.out.print("send " + xml + "\n");

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            bw.write(xml + "\n" + "<break/>" + "\n");
            bw.newLine();
            bw.flush();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}