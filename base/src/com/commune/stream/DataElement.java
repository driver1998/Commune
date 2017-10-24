package com.commune.stream;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import java.io.*;
import java.net.Socket;

public interface DataElement {

    String getXML();

    static DataElement getElement(String xmlString) throws InvalidElementException {
        try {
            Document document = DocumentHelper.parseText(xmlString);
            org.dom4j.Element root = document.getRootElement();
            String name = root.getName();

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

        } catch (DocumentException ex) {
            System.out.println(xmlString);
            throw new InvalidElementException("格式错误");
        }
    }


    static DataElement getElement(Socket socket) throws IOException, InvalidElementException {
        try  {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            StringBuilder sb = new StringBuilder();

            while ((line=br.readLine())!=null) {
                if (line.equals("<break/>")) break;
                sb.append(line);
                System.out.println(line);
            }

            System.out.println("captured here");
            return getElement(sb.toString());
        } catch (IOException ex) {
            throw ex;
        }
    }

    default void send(Socket socket) {
        try  {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bw.write(getXML() + "\n" + "<break/>" + "\n");
            bw.newLine();
            bw.flush();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}